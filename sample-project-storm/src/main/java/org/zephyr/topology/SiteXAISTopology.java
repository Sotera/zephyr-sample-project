package org.zephyr.topology;

import org.zephyr.bolt.HdfsInputBolt;
import org.zephyr.data.Entry;
import org.zephyr.data.Record;
import org.zephyr.storm.bolt.OutputterBolt;
import org.zephyr.storm.bolt.ParsingBolt;
import org.zephyr.storm.bolt.SchemaBolt;
import org.zephyr.storm.data.EntrySerializer;
import org.zephyr.storm.data.RecordSerializer;

import storm.kafka.KafkaConfig;
import storm.kafka.KafkaConfig.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.generated.StormTopology;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseBasicBolt;

public class SiteXAISTopology {
	
	private String confFile;
	
	public SiteXAISTopology() {
		this.confFile = "conf/sitexais-topo.xml";
	}
	
	private IRichSpout getKafkaSpout() {
		BrokerHosts hosts = new KafkaConfig.ZkHosts("arcus2:2181,arcus3:2181,arcus4:2181", "/kafka/brokers");
		SpoutConfig spoutConfig = new SpoutConfig(hosts, "hdfsPaths", "/kafka-zephyr", "discovery");
		return new KafkaSpout(spoutConfig);
	}
	
	private BaseBasicBolt getHdfsInputBolt() {
		return new HdfsInputBolt("hdfs://arcus1:8020");
	}
	
	public StormTopology buildTopology() {
		TopologyBuilder builder = new TopologyBuilder();
		builder.setSpout("kafka-hdfs-ls-spout", getKafkaSpout(), 1);
		builder.setBolt("hdfs-input-bolt", getHdfsInputBolt(), 4).shuffleGrouping("kafka-hdfs-ls-spout");
		builder.setBolt("parsing-bolt", new ParsingBolt(confFile, "parserFactory"), 8).shuffleGrouping("hdfs-input-bolt");
		builder.setBolt("schema-bolt", new SchemaBolt(confFile, "schema"), 5).shuffleGrouping("parsing-bolt");
		builder.setBolt("outputter-bolt", new OutputterBolt(confFile, "hdfsOutputterBolt"), 8).shuffleGrouping("schema-bolt");
		return builder.createTopology();
	}
	
	public static void main(String args[]) throws AlreadyAliveException, InvalidTopologyException {
		StormTopology topo = new SiteXAISTopology().buildTopology();
		Config conf = new Config();
		conf.setDebug(true);
		conf.setNumWorkers(64);
		Config.registerSerialization(conf, Record.class, RecordSerializer.class);
		Config.registerSerialization(conf, Entry.class, EntrySerializer.class);
		StormSubmitter.submitTopology("sitexais-topo", conf, topo);
	}
	
}
