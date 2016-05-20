#!/bin/bash
#clean environment
./clean.sh

#create a volume container dbstore, mounting data/ of host to /data/ of container
cd dbstore
docker build -t dbstore .
cd ..
docker run --name dbstore dbstore

#create an image from Dockerfile
#it takes too long to install JDK
docker build -t kaiwen/ubuntu .

#create network
docker network create -d bridge my-bridge-network

#mount volumes of dbstore to newly created client and server
docker run -td --volumes-from dbstore --net=my-bridge-network --name server kaiwen/ubuntu /data/private/pingpong/runserver.sh
docker run -td --volumes-from dbstore --net=my-bridge-network --name client kaiwen/ubuntu /data/private/pingpong/runclient.sh

#check output OK
#./verify.sh

./readLogs.sh &

#stop server
#docker stop client server dbstore
