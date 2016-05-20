#!/bin/bash
cd /data/private/pingpong
javac PingPongClient.java > /dev/null
java PingPongClient server 9876
