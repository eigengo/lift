# Spark
FROM martinz/spark-base:latest

# Instead of using a random port, bind the worker to a specific port
ENV SPARK_WORKER_PORT 8888
EXPOSE 8888

ADD files /root/spark_worker_files

# Add the entrypoint script for the master
CMD ["-h"]
RUN chown root.root /root/spark_worker_files/default_cmd
RUN chmod 700 /root/spark_worker_files/default_cmd

ENTRYPOINT ["/root/spark_worker_files/default_cmd"]