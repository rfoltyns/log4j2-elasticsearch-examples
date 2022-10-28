#!/bin/sh

java -Dhostname=somehostname -Dlog4j.configurationFile=log4j2-async-es-7-with-virtual-properties.xml -jar log4j2-elasticsearch-ahc-one-jar/target/log4j2-elasticsearch-hc-one-jar-0.0.1-SNAPSHOT.one-jar.jar
