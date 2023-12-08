#!/bin/bash
java -jar iottunnelclient-1.0-SNAPSHOT-shaded.jar \
--endpoint a1cw44obfpxk2h-ats.iot.ap-southeast-1.amazonaws.com --client_id sdk-java --topic sdk/test/java --ca_file ./certs/root-CA.crt --cert ./certs/zg_d1.cert.pem --key ./certs/zg_d1.private.key --thing zg_d1
