

This is my projects of  (Distributed System) at UCSD.



## project 0 - Configure a Docker cluster
Pracitce using Docker. Create docker containers as storage, server and client sharing a volume, and pingpong C programs testing the virtual machines' connectioin.

## project 1 - Java RMI library
Implement a Java RMI (Remote Method Invocation) library, using Java dynamic proxy and Java reflection. The RMI library can be used for any general purpose remote service. Multithreading and synchronization among services are carefully taken into consideration.

## project 2 - Distributed File System
This project implements a simple distributed filesystem using the RMI library. Files will be hosted remotely on one or more storage servers. Separately, a single naming server will index the files, indicating which one is stored where. When a client wishes to access a file, it first contacts the naming server to obtain a stub for the storage server hosting it. After that, it communicates directly with the storage server to complete the operation. When read Read-write synchronization and consistency are very important in this distributed filesystem.

## project 3 - Configure a Hadoop cluster
Use Docker to deploy a Hadoop cluster with one master and four slaves. Run a Hadoop Java program BigramCount (similar to classic WordCount) on the Hadoop cluster.

## hw1
Homework 1

## hw2
Homework 2
