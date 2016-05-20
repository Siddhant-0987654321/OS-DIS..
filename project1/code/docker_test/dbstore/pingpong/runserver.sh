#!/bin/bash
cd /data/private/pingpong
javac PingPongServer.java > /dev/null
java PingPongServer 9876
