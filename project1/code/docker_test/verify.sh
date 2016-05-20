#!/bin/bash
echo waiting for completion of data transimission...
#sleep 35
countOK=$(./readLogs.sh | grep -c OK)
countMS=$(./readLogs.sh | grep -c MISSING)
echo In client\'s log,

echo 
if [ "$countOK" = "10" ] && [ "$countMS" = "0" ]; then
	echo -e "\e[32m=========================================================\e[0m"
	echo -e "\e[32m$countOK OK found, $countMS MISSING found, as expected.\e[0m"
	echo -e "\e[32m=========================================================\e[0m"
else
	echo -e "\e[31m=========================================================\e[0m"
	echo -e "\e[31m$countOK OK found, $countMS MISSING found\e[0m"
	echo -e "\e[31m=========================================================\e[0m"
fi
echo 
