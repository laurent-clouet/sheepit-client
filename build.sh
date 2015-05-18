#!/bin/bash

jvm_name="openjdk-1.7.0-u80-unofficial-windows-amd64-image"
mkdir jre
wget https://bitbucket.org/alexkasko/openjdk-unofficial-builds/downloads/$jvm_name.zip
unzip $jvm_name.zip
cp -rf $jvm_name/* jre/
rm -rf jre/include jre/src.zip
wget https://www.sheepit-renderfarm.com/media/applet/client-latest.php -O jre/sheepit-client.jar
cd jre
7z a ../application.7z .
cd ..
cat starter.sfx config.cfg application.7z > sheepit-wrapper.exe
rm -rf application.7z jre  $jvm_name $jvm_name.zip
