# UCSD CSE291e projects

This is my projects of CSE291e (Distributed System) at UCSD. I will push my project after the due.

## project 0
Pracitce using Docker. Create docker containers as storage, server and client sharing a volume, and pingpong C programs testing the virtual machines' connectioin.

## project 1
Implement a Java RMI (Remote Method Invocation) library, using Java dynamic proxy and Java reflection. The RMI library can be used for any general purpose remote service. Multithreading and synchronization among services are carefully taken into consideration.

## project 2 (not completed yet)
This project implements a simple distributed filesystem using the RMI library. Files will be hosted remotely on one or more storage servers. Separately, a single naming server will index the files, indicating which one is stored where. When a client wishes to access a file, it first contacts the naming server to obtain a stub for the storage server hosting it. After that, it communicates directly with the storage server to complete the operation.

## project 3 (not started yet_
