# Spark 1.2.0
# Version 1.0.0
#
FROM sequenceiq/hadoop-docker:2.6.0

# Add config files and configure script
ADD files /root/spark_files

ENV SCALA_VERSION 2.11.4
ENV SPARK_VERSION 1.2.0
ENV SCALA_HOME /opt/scala-$SCALA_VERSION
ENV SPARK_HOME /opt/spark-$SPARK_VERSION
ENV PATH $SPARK_HOME:$SCALA_HOME/bin:$PATH

# Spark settings default values
ENV SCALA_HOME /opt/scala-2.11.4
ENV SPARK_EXECUTOR_MEMORY 1500m
ENV SPARK_DRIVER_MEMORY 1500m
ENV SPARK_WORKER_MEMORY 1500m
ENV SPARK_MASTER_MEM 1500m
ENV HADOOP_HOME "/etc/hadoop"
ENV SPARK_LOCAL_DIR /tmp/spark

# install a few other useful packages plus Open Jdk 7
#RUN yum install -y wget unzip zip tar git \
#    java-1.6.0-openjdk-devel java-1.7.0-openjdk-devel
RUN yum -y -q install java-1.8.0-openjdk java-1.8.0-openjdk-devel
ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-openjdk

#apt-get update && apt-get upgrade -y && apt-get install -y less openjdk-7-jre-headless net-tools vim-tiny sudo openssh-server iputils-ping python2.7

# Install Scala
ADD http://www.scala-lang.org/files/archive/scala-$SCALA_VERSION.tgz /
RUN (cd / && gunzip < scala-$SCALA_VERSION.tgz)|(cd /opt && tar -xvf -)
RUN rm /scala-$SCALA_VERSION.tgz

# Install Spark
ADD http://d3kbcqa49mib13.cloudfront.net/spark-$SPARK_VERSION-bin-hadoop2.4.tgz /
RUN (cd / && gunzip < spark-$SPARK_VERSION-bin-hadoop2.4.tgz)|(cd /opt && tar -xvf -)
RUN (ln -s /opt/spark-$SPARK_VERSION-bin-hadoop2.4 /opt/spark-$SPARK_VERSION && rm /spark-$SPARK_VERSION-bin-hadoop2.4.tgz)
RUN (ln -s /opt/spark-$SPARK_VERSION-bin-hadoop2.4 spark)

# Spark configuration
RUN (rm -rf /opt/spark-$SPARK_VERSION/work)
RUN (mkdir -p /opt/spark-$SPARK_VERSION/work)
RUN (mkdir /tmp/spark)
RUN (rm -rf /var/lib/hadoop/hdfs)
RUN (mkdir -p /var/lib/hadoop/hdfs)
RUN (rm -rf /opt/spark-$SPARK_VERSION/logs)
RUN (mkdir -p /opt/spark-$SPARK_VERSION/logs)
RUN (cp /root/spark_files/spark-env.sh /opt/spark-$SPARK_VERSION/conf/)
RUN (cp /root/spark_files/log4j.properties /opt/spark-$SPARK_VERSION/conf/)