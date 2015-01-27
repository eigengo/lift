# Spark
FROM martinz/spark-base:latest

# Expose TCP ports 7077 8080
EXPOSE 7077 8080

ADD files /root/spark_master_files

RUN chown root.root /root/spark_master_files/default_cmd
RUN chmod 700 /root/spark_master_files/default_cmd

CMD ["/root/spark_master_files/default_cmd"]