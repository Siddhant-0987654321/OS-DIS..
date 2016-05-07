#!/bin/bash
#clean environment
./clean.sh

#create a volume container dbstore, mounting data/ of host to /data/ of container
cd dbstore
docker build -t dbstore .
cd ..
docker run --name dbstore dbstore

#create an image from Dockerfile
docker build -t kaiwen/ubuntu .

#copy client and server to data volume
#./copyfiles.sh #now achieved by Dockerfile VOLUME
 
#create network
docker network create -d bridge my-bridge-network

#mount volumes of dbstore to newly created client and server
docker run -td --volumes-from dbstore --net=my-bridge-network --name server kaiwen/ubuntu /data/.private/cater/catserver /data/string.txt 2000
docker run -td --volumes-from dbstore --net=my-bridge-network --name client kaiwen/ubuntu /data/.private/cater/catclient /data/string.txt 2000

#check output OK
./verify.sh

#stop server
docker stop server server dbstore
