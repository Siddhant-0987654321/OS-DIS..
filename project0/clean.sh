#!/bin/bash
docker stop server client dbstore
docker rm server client dbstore
docker rmi server client dbstore
docker volume rm data
docker volume rm $(docker volume ls -qf dangling=truea)
docker rmi kaiwen/ubuntu
docker network rm my-bridge-network
