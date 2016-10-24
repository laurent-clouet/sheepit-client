# SheepIt Render Farm Client

## Overview

SheepIt Render Farm Client is an *Open Source* client for the distributed render farm [**SheepIt**](https://www.sheepit-renderfarm.com).

## Compilation

You need Java 1.7. OpenJDK and Oracle are both supported.
You also need [ant](http://ant.apache.org/).
To create the jar file, simply type `ant` in the project's root directory.

## Usage

Once you have a jar file, you can view the usage by running:

    java -jar bin/sheepit-client.jar --help

When you are doing development work, you can use a mirror of the main site specially made for demo/dev. The mirror is located at **http://sandbox.sheepit-renderfarm.com**, and you can use it by passing `-server http://www-demo.sheepit-renderfarm.com` to your invocation of the client.
