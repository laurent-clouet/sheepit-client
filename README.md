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

When you are doing development work, you can use a mirror of the main site specially made for demo/dev. The mirror is located at **http://sandbox.sheepit-renderfarm.com**, and you can use it by passing `-server http://sandbox.sheepit-renderfarm.com` to your invocation of the client.

At the command line ui (-ui text / -ui oneLine) you could type in the following commands and press enter to controll the client:

* block: to block the current project
* block_time n: to automated block projects needing more than n minutes to finish rendering
* pause: pause the client to request new jobs after the current frame has finished to render
* resume: resume the client after it was paused
* stop: stop the client after the current frame has finished
* cancel: cancle the stop request
* quit: stops the client directly without finishing the current frame
