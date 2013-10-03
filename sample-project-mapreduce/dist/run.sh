#!/bin/bash

if [ -f lib/resources.jar ]
        then
                rm lib/resources.jar
fi
jar -cf lib/resources.jar -C conf/ .

export HADOOP_CLASSPATH=lib/*:$HADOOP_CLASSPATH

hadoop jar lib/zephyr-mapreduce-0.1.0.jar org.zephyr.mapreduce.ZephyrDriver "${@:1}"