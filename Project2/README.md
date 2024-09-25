Project1 README: axk210197

Overview

This project implements a distributed system consisting of nodes with a specified topology and communication protocol.

- Main.cpp: The main program that takes input for network topology and creates nodes accordingly.
- Node.cpp and Node.h: Implement the logic for each node based on the MAP protocol.
- Log.cpp and Log.h: Provide logging functionality for the nodes.
- launcher.sh: A shell script to launch the program on remote machines.
- cleanup.sh: A shell script to stop and clean up processes on remote machines.
- build.sh: A shell script to compile the C++ program.
- cleanfiles.sh: A shell script to remove compiled files.


Compilation

To compile the C++ program, run the following command:

./build.sh

This will generate the executable for Project1.

Running the Program

1. Use the correct config file.

2. Launch the program on remote machines using the launcher.sh script. 

./launcher.sh

3. The program will execute on the specified remote machines, simulating the distributed system based on the provided configuration.

Cleanup

To stop and clean up processes on remote machines, use the cleanup.sh script. 

./cleanup.sh

This script will terminate the running processes associated with the program.

Node Class Modifications:
The Node class in Node.cpp has been modified to include vector clocks.
Functions for updating vector clocks when sending and receiving messages have been implemented.

Vector Timestamps:
Vector timestamps are attached to outgoing messages and extracted from incoming messages.
Local state recording now includes vector timestamps.

Snapshot Consistency Verification:
Node 0 (or the root node) uses vector timestamps to verify the consistency of snapshots.

Integration with Existing Code:
The changes related to vector clocks and timestamps are integrated into the existing codebase.
Please review the updated Node.cpp, Node.h, and main.cpp files for the detailed implementation of vector clocks and timestamps.
