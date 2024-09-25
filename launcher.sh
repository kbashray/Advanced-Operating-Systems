#!/bin/bash

# Change this to your netid [CHANGE THIS]
netid=axk210197

# Root directory of project [CHANGE THIS]
PROJDIR=/home/011/a/ax/axk210197/project1

# Directory where the config file is located on your local system [CHANGE THIS]
CONFIGLOCAL=/home/011/a/ax/axk210197/config.txt

# Directory your compiled classes are in [CHANGE THIS if you move the classes]
BINDIR=$PROJDIR

# Your main project executable [CHANGE THIS if you rename the project]
PROG=Project1

# Extension for hosts [CHANGE THIS if using a different host system (setting to "" should suffice)]
hostExtension="utdallas.edu"

# Run command [CHANGE THIS if you want a different pattern or use a different software]
runCommand="$BINDIR/$PROG $CONFIGLOCAL"
# Remove $CONFIGLOCAL if you don't want to give your program the configuration file path as an argument

# Loop through hosts, remove comment lines starting with # and $, and any carriage returns
n=0
# Remove comments | Remove other comments | Remove carriage returns
cat "$CONFIGLOCAL" | sed -e "s/#.*//" | sed -e "/^\s*$/d" | sed -e "s/\r$//" |
(
    # Read the first valid line and collect only the number of hosts
    read i
    echo "$i"
    ii=$(echo "$i" | awk '{ print $1 }')
    echo "Hosts: $ii"

    # For each host, loop
    while [[ $n -lt $ii ]]
    do
        # Read the port number and host address
        read line
        p=$(echo "$line" | awk '{ print $1 }')
        host=$(echo "$line" | awk '{ print $2 }')

        # Add host extension to string if missing from domain name
        if [[ "$host" != *"$hostExtension"* ]]; then
            host="$host.$hostExtension"
        fi
        echo "$host"

        # Issue command
        echo "Issued command: gnome-terminal -e \"ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host $runCommand; exec bash\" &"
        gnome-terminal -e "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host $runCommand; exec bash" &
        sleep 1

        # Increment loop counter
        n=$(( n + 1 ))
    done
)

echo "Launcher complete"
