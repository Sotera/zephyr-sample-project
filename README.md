Sample Project README
========================

This Sample Project should serve as a starting point for your own implementation of Zephyr.  In it you will find examples (some mature, some... less mature) of best ways of going about
using Zephyr in:
 - A Map Reduce framework
 - A Storm framework
 - Standalone (ultimately, this is for testing purposes, though you could shove it into a non-distributed system if you wanted)
 
While we certainly intend on polishing the Storm and Spark Streaming portions more before the first release, they DO work now.  However, don't be too surprised if it changes and your 
subsequent usage must change as well.  In specific, the Storm portion has a very ugly configuration process for the ZephyrDriver..
 
Requirements
--------------------
 - Gradle
 - Data (CSV is best, for now)
 
Sample Project Core
--------------------
In sample-project-core, you'll find a few example implementations of the various capabilities that Zephyr offers in the form of a few defined Schemas and their associated extentions.
For example, you'll find an example of a preprocessor in the PanoramioPreprocessor.  You'll find a ParserFactory implementation in the TwitterParserFactory class and its associated TwitterParser.

You'll also see a few Enrichers, mostly on the Twitter feed.

Before you dive on in, let me give you a bit of a tour of this project.

We'll use sample-project-mapreduce and sample-project-core for this tour, and we'll go over how we've constructed a Zephyr ingestion job for Twitter data.

To start with, we'll start at the final bit of glue - under sample-project-mapreduce/resources/distributed-cache/resources/twitter-job.xml.

This file define our job flow to our ZephyrDriver.  It gives our job a name, defines the input and output locations, and allows us to specify what ParserFactory to use, what Schema to use, what 
Enrichers we'd like to use, and our OutputFormat we'd like to use.

The way our flow is going to operate, at a high level, is:
 - Raw data is placed in a specific location in HDFS
 - Job xml is configured to use this as the input directory
 - ZephyrDriver is kicked off with the argument "-job twitter-job.xml" passed to it
 - It defines the Zephyr Map Reduce job to the jobtracker
 - The ZephyrMapper runs over the data, reading it in from HDFS, preprocessing it (if defined), parsing it, applying a schema to the parsed results (naming, typing, applying a visiblity, normalizing and validating)
 	, running N optional enrichers over it, formatting the data, then passing that data to the Mapper's OutputCollector.  
 - The output is written out to a file in HDFS as per the job xml configuration settings.
 
So, if you look at our twitter-job.xml file now, you will see that we have no preprocessor defined, but we do have a TwitterParserFactory specified.  We also define the schema to use, but
you'll also notice that it is is a "ref" - meaning we've defined it in a different location in either this Spring configuration file, or another (it's in another - sample-project-core/resources/distributed-cache/resources/twitter-schema.xml
to be precise).  We'll look at a Schema definition later.

We also define a list of enrichers to use - and these enrichers happen in order.  It's important to note that if an enricher fails, we ignore that it failed, and we pass the Record along
to the next enricher anyway.

Lastly, we define an output formatter - a HiveOutputFormatter, to be specific - and we configure it with the order and number of fields we're expecting to write out.  Zephyr does not carry
empty fields around in the Record - this would cause issues with some output destinations.  Instead we don't pass anything at all, but we do are declaring to our HiveOutputFormatter what 
fields we'd like to see.  If it doesn't have any, it can output a blank on its own.

This description is all well and good, but what about the Schema? If you look near the top of the twitter-job.xml file, you'll see a line:
	<import resource="classpath:twitter-schema.xml" />
	
This is important because our core code - the Parsers, Outputters, Preprocessors, Enrichers - these are all platform agnostic.  They don't care if you're running in MR or Storm or Spark or on a Potato.
Anything in the ETL process that is *not* strictly "reading data (physical)", "distributing data", or "writing data" is in the "sample-project-mapreduce|storm|standalone|spark-streaming" projects.  
Everything else - the crux of the ETL process - is in sample-project-core (and zephyr-core).  

So, our schema definition is in sample-project-core/resources/distributed-cache/resources/twitter-schema.xml.  This is because this schema will be the same *regardless* of what platform you run Zephyr on. 
The only thing that changes is the reading of the data, the distribution of the data, and what you do with that data after you've done your Extract and Transform (otherwise known as the Load).

If you now open that twitter-schema.xml file, you'll see how we define our schema.  One of the first things you'll see in this file is the definition of a Schema.  Aside from the feedName property, 
you pretty much don't need to touch anything in this definition (no extra functionality is gained by tweaking it, so I'd just leave it alone unless you have a brilliant master plan, in which case, I wish you the best).

However, you'll notice it references a ref called "required-set" - this is the set of *raw field names* that we will have required be validated and normalized and be added to the Record or we won't 
consider this record valid.  A great example of this is right here in the Twitter data - what is the point in a Twitter record if it doesn't at least have an id, dtg, user_id, or text body?  So, the
system will drop any record that doesn't at the least contain all 4 of these fields (the others are optional).

Below this, you'll see a reference to schemata-list - this defines to the Schema what we want to map from a rawFieldName, to what new label, and we allow you to specify a few fields.  

By default, no Normalizers are specified, and by default, the Validators are set to "nonEmptyValidator" - again, we just don't want empty fields going through the system (at least, not by default).

However, when you scroll down you'll see some different configurations.  For example, on the "latitude" mapping, we have:

			<property name="metadata" value="primary" />
			<property name="preNormalizationValidator" ref="decimalDegreesLatitudeValidator" />
			<property name="postNormalizationValidator" ref="decimalDegreesLatitudeValidator" />
			
By default, the system applies a metadata field of empty string to each Entry.  Also, we want to make sure that our entry has valid latitude for it - so we validate it.  These validators are
defined in the zephyr/zephyr-core/src/main/resources/zephyr-core-config.xml file - mostly it just defines non-configurable Validators in one place only.

The reason why we have two validators?  It's very possible that your first validation routine will be to specify the field as nonEmptyValidator.  Then you may run 0..N Normalizers over it, in sequence.  If any
of those fail, we may want to validate again at the end - to double-dog check that our data is fine.

If you won't be normalizing, but you only want one validator to run - you can easily add an alwaysTrueValidator for the preNormalizationValidator property, and your postNormalizationValidator can be your last, 
big, actual validation routine.  There's flexibility there.  The big reason for a Pre Normalization Validator?  Why try to normalize at all if the data is incorrect to start with? 
Effectively, it can speed performance up.

Another field, that you won't see (but, if you are working on a secure system, desperately want), is this property:
			<property name="visibility" value="AC1&AC2&AC3" />
			
This property allows you to specify an Entry by Entry level visibility tag.  

Thus ends our tour of our configuration.  Now let's talk about Gradle and how you're going to build your project.

Gradle Best Practices (with Zephyr)
------------------------

For starters, I am no Gradle expert.  I basically know enough to not kill myself or others.  However, Gradle is so much nicer than maven that it isn't even funny.  For starters,
take a look at sample-project/build.gradle and sample-project/settings.gradle.  That is, by itself, so much smaller than a maven pom.  Short and sweet.

If you are going to make your own project, and don't want to name it "sample project", you can still make a copy of this repository and name it and the folders what you want.  The big key
is going to be to rename the "sample-project-*" in each one of the build.gradle or settings.gradle folders.  Delete the ones that don't make sense for your project (if you're doing Storm only, you only need
sample-project, sample-project-core, and sample-project-storm). 

The big thing for MapReduce is in the sample-project/sample-project-mapreduce/build.gradle file - in it you will notice that we've defined some jars as "hadoopProvided".  Basically, this acts as
maven's provided scope - only it's more specific.  This is lovely if only because when doing a mapreduce job, you don't really want or need to package up Hadoop's jar files - it already has them.

However, if you're doing a Storm job - you most certainly WILL want Hadoop's jars (assuming you are using HDFS, at any rate).

Take a look at our distResources and distJars tasks - they both grab the files they need and put them into a nice neat dist folder for us.  Then we can just deploy that folder to our cluster 
and can run our files from there.

While you don't HAVE to use gradle to manage your distributions, I can attest that it makes things very easy.  But, if you'd rather just use gradle for doing builds and you'll do your distributions 
by hand or with a bash script, that's up to you. 

Gotchas
-----------------------

Zephyr Storm works - in standalone.  It might no be super pretty and elegant yet, but it does work - and the actual ET part of Zephyr is very mature, so the most you have to worry about is 
the zephyr-storm scaffolding.  

I hope that's enough for you to get started.  Please feel free to contact me at dwayne.pryce@soteradefense.com.  Eventually, we'll create a mailing list to handle this stuff.