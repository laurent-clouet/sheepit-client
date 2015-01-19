#!/bin/bash

mkdir jre
wget https://bitbucket.org/alexkasko/openjdk-unofficial-builds/downloads/openjdk-1.7.0-u60-unofficial-windows-amd64-image.zip
unzip openjdk-1.7.0-u60-unofficial-windows-amd64-image.zip
cp -rf openjdk-1.7.0-u60-unofficial-windows-amd64-image/* jre/
rm -rf jre/include jre/src.zip
wget https://www.sheepit-renderfarm.com/media/applet/client-latest.php -O jre/sheepit-client.jar
cd jre
7z a ../application.7z .
cd ..
cat starter.sfx config.cfg application.7z > sheepit-wrapper.exe
rm -rf application.7z jre  openjdk-1.7.0-u60-unofficial-windows-amd64-image openjdk-1.7.0-u60-unofficial-windows-amd64-image.zip
