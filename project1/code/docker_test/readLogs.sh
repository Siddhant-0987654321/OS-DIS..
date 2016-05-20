#!/bin/bash
echo 
echo -e "\e[32mCLIENT LOGS:\e[0m"
docker logs --follow client
echo -e "\e[32mSERVER LOGS:\e[0m"
docker logs --follow server
