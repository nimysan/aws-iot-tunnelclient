#!/bin/bash
#Filename start.sh
mvn clean package
mvn -X exec:java -Dexec.mainClass=top.cuteworld.iotdemo.SSHTunnelDaemon -Dexec.args='--endpoint a1cw44obfpxk2h-ats.iot.ap-southeast-1.amazonaws.com --client_id sdk-java --topic sdk/test/java --ca_file ./certs/root-CA.crt --cert ./lawn_mower_1.cert.pem --key ./lawn_mower_1.private.key --thing lawn_mower_1'
