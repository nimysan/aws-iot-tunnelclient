#!/bin/bash

mvn clean package
scp target/iot*.jar amazon@192.168.1.117:~/iot
scp certs/* amazon@192.168.1.117:~/iot/certs
scp start.sh amazon@192.168.1.117:~/iot