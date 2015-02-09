#!/bin/bash

function create_spark_directories() {
    rm -rf /opt/spark-$SPARK_VERSION/work
    mkdir -p /opt/spark-$SPARK_VERSION/work
    mkdir /tmp/spark
    # this one is for Spark shell logging
    rm -rf /var/lib/hadoop/hdfs
    mkdir -p /var/lib/hadoop/hdfs
    rm -rf /opt/spark-$SPARK_VERSION/logs
    mkdir -p /opt/spark-$SPARK_VERSION/logs
}

function deploy_spark_files() {
    cp /root/spark_files/spark-env.sh /opt/spark-$SPARK_VERSION/conf/
    cp /root/spark_files/log4j.properties /opt/spark-$SPARK_VERSION/conf/
}

function configure_spark() {
    sed -i s/__SPARK_LOCAL_IP__/$2/ /opt/spark-$SPARK_VERSION/conf/spark-env.sh
    sed -i s/__MASTER__/$1/ /opt/spark-$SPARK_VERSION/conf/spark-env.sh
    #sed -i s/__MASTER__/master/ /opt/spark-$SPARK_VERSION/conf/spark-env.sh
    sed -i s/__SPARK_HOME__/"\/opt\/spark-${SPARK_VERSION}"/ /opt/spark-$SPARK_VERSION/conf/spark-env.sh
    sed -i s/__JAVA_HOME__/"\/usr\/lib\/jvm\/java-1.8.0-openjdk.x86_64"/ /opt/spark-$SPARK_VERSION/conf/spark-env.sh
}

function prepare_spark() {
    echo "preparing spark master $1, self $2"
    create_spark_directories
    deploy_spark_files
    configure_spark $1 $2
}