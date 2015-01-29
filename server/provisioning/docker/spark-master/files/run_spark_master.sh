#!/bin/bash
/opt/spark-1.1.0/sbin/start-master.sh

while [ 1 ];
do
	tail -f /opt/spark-${SPARK_VERSION}/logs/*.out
        sleep 1
done