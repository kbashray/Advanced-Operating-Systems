#!/bin/bash

# Command to grant permission to file to run [RUN THIS]: chmod +x build.sh

# Code to remove carriage returns from files
find . -name "*.java" -type f -exec sed -i 's/\r$//' {} \;

# Compilation command [CHANGE THIS to match your project files]
javac CheckpointingSimulation.java CheckpointMessage.java ConfigParser.java Network.java Node.java NodeState.java Operation.java

echo "Done building."
