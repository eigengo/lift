#!/bin/bash
. /opt/spark-1.1.0/conf/spark-env.sh
${SPARK_HOME}/bin/spark-class org.apache.spark.deploy.worker.Worker spark://localhost:7077 -i $SPARK_LOCAL_IP