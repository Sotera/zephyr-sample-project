package org.zephyr.bolt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import backtype.storm.task.TopologyContext;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class HdfsInputBolt extends BaseBasicBolt {

	private static final long serialVersionUID = -2114919302981292299L;
	private String nameNode;
	private FileSystem fs;
	
	public HdfsInputBolt(String nameNode) {
		this.nameNode = nameNode;
	}
	
	@SuppressWarnings("rawtypes")
	public void prepare(Map stormConf, TopologyContext context) {
		super.prepare(stormConf, context);
		Configuration config = new Configuration();
		config.set("fs.defaultFS", nameNode);
		try {
			fs = FileSystem.get(config);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void execute(Tuple input, BasicOutputCollector collector) {
		String path = input.getStringByField("path");
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(fs.open(new Path(path))));
			collector.emit(new Values(reader.readLine()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("record"));
	}

}
