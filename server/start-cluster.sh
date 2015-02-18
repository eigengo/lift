#!/bin/bash

echo "Starting Cassandra"
docker run --name cassandra --net="host" --expose 7199 --expose 7000 --expose 7001 --expose 9160 --expose 9042 --expose 22 --expose 8012 --expose 61621 spotify/cassandra &

echo "Waiting for Cassandra to start"
sleep 10

echo "Starting main monolith 3 nodes"
docker run --name main --net="host" -e "HOST=192.168.59.103" -e "APP_PORT=2551" -e "REST_PORT=8081" -e "SEED_NODES=akka.tcp://Lift@192.168.59.103:2551" -e "APP_ADDR" -e "JOURNAL=192.168.59.103" -e "SNAPSHOT=192.168.59.103" -p 2551:2551 -p 8081 janm399/lift:main-production &
docker run --name main2 --net="host" -e "HOST=192.168.59.103" -e "APP_PORT=2552" -e "REST_PORT=8081" -e "SEED_NODES=akka.tcp://Lift@192.168.59.103:2551" -e "APP_ADDR" -e "JOURNAL=192.168.59.103" -e "SNAPSHOT=192.168.59.103" -p 2552:2552 -p 8081 janm399/lift:main-production &
docker run --name main3 --net="host" -e "HOST=192.168.59.103" -e "APP_PORT=2553" -e "REST_PORT=8081" -e "SEED_NODES=akka.tcp://Lift@192.168.59.103:2551" -e "APP_ADDR" -e "JOURNAL=192.168.59.103" -e "SNAPSHOT=192.168.59.103" -p 2553:2553 -p 8081 janm399/lift:main-production &