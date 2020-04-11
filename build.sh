#!/bin/bash

jvm_name="jdk-11.0.6+10-jre"

#creating folder structure
mkdir jre
mkdir jre/jre

#Pull latest client
wget https://www.sheepit-renderfarm.com/media/applet/client-latest.php -O jre/sheepit-client.jar

#unzip jre
unzip $jvm_name.zip
cp -rf $jvm_name/* jre/jre/

#Building the exe bundle
cd jre
7z a ../application.7z .
cd ..
cat starter.sfx config.cfg application.7z > sheepit-wrapper.exe

#Cleanup
rm -rf application.7z jre  $jvm_name
