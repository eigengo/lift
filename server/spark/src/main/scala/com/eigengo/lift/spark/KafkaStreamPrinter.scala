package com.eigengo.lift.spark

import com.typesafe.config.{ConfigFactory, Config}
import org.apache.log4j.{Logger, Level}
import org.apache.spark.{SparkContext, SparkConf, Logging}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka.KafkaUtils

object KafkaStreamPrinter extends App with Logging {

  private def kafkaConsumerConfig(config: Config) = config.getConfig("kafka.consumer")

  override def main(args: Array[String]): Unit = {
    val log4jInitialized = Logger.getRootLogger.getAllAppenders.hasMoreElements
    if (!log4jInitialized) {
      logInfo("Setting log level to [WARN] for streaming example." +
        " To override add a custom log4j.properties to the classpath.")

      Logger.getRootLogger.setLevel(Level.WARN)
    }

    val config = ConfigFactory.load()

    val context = new SparkContext(new SparkConf()
      .setAppName("Basic job")
      .setMaster(config.getString("spark.master")))

    context.parallelize(1 to 1000).map(_ + 1).foreach(println)

    /*val config = ConfigFactory.load()

    val ssc = new StreamingContext(
      new SparkConf()
        .setAppName("Kafka Stream Printer")
        .setMaster(config.getString("spark.master")), Seconds(30))

    ssc.checkpoint("checkpoint")

    //TODO: Configurabla parallelism
    val numDStreams = 5

    logWarning("Driver connecting to zookeeper " + kafkaConsumerConfig(config).getString("zkConnect"))
    logWarning("Driver connecting to master " + config.getString("spark.master"))

    //val kafkaDStreams = (1 to numDStreams).map { _ =>
    KafkaUtils.createStream(
      ssc,
      kafkaConsumerConfig(config).getString("zkConnect"),
      kafkaConsumerConfig(config).getString("groupId"),
      Map(kafkaConsumerConfig(config).getString("topic") -> 3),
      StorageLevel.NONE)
        .map(x => s"key: ${x._1} value: ${x._2}")
        .foreachRDD(x => {
          x.foreach(y => {
            println(y)
            Logger.getLogger("KafkaStreamPrinter").info(s"READING FROM KAFKA $y")
          })
        }
      )
    //}   .

    ssc.start()
    ssc.awaitTermination()
    */
  }
}