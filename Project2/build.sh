#!/bin/bash

# Command to grant permission to file to run [RUN THIS]: chmod +x build.sh

# Code to remove carriage returns from files
find . -name "*.java" -type f -exec sed -i 's/\r$//' {} \;

# Compilation command [CHANGE THIS to match your project files]
javac RoucairolCarvalho.java MyRCServer.java MyRCClient.java MyMessage.java

echo "Done building."
