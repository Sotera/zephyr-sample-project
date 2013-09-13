package org.zephyr.spark.streaming

object StreamingDriver extends App {
  val springContext = new ClassPathXmlApplicationContext("zehpyr-streaming-context.xml")
  val zephyrStreamer = springContext.getBean("zephyrStreamer") match {
    case zephyrStreamer : ZephyrStreamer => zephyrStreamer
    case _ => throw new RuntimeException("Unable to retrieve zephyrStreamer from Spring context")
  }
  zephyrStreamer.run
}
