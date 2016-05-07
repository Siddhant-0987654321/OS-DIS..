#!/bin/bash
#This script is not used in the lastest version
sudo mkdir -p /var/lib/docker/volumes/data/_data/.private/
sudo cp ./cater  /var/lib/docker/volumes/data/_data/.private -r
sudo cp string.txt /var/lib/docker/volumes/data/_data/string.txt
