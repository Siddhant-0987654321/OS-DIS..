#!/bin/sh
make
for i in `seq 1  100`
do
	java conformance.ConformanceTests | grep -v "passed: 18" | grep -100 fail
	echo $i "test finished"
done
