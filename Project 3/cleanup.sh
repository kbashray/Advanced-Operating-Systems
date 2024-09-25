#!/bin/bash

# Change this to your netid [CHANGE THIS]
netid=axk210197

# Root directory of project [CHANGE THIS]
PROJDIR=/home/011/a/ax/axk210197/project3

# Directory where the config file is located on your local system [CHANGE THIS]
CONFIGLOCAL=/home/011/a/ax/axk210197/project3/config.txt

# Extension for hosts [CHANGE THIS if using a different host system (setting to "" should suffice)]
hostExtension="utdallas.edu"

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
        # Collect the name of the host from the file; collect the second element on the line as a host
        echo "Next host:"
        read line
        host=$(echo "$line" | awk '{ print $2 }')

        # Add host extension to string if missing from domain name
        if [[ "$host" != *"$hostExtension"* ]]; then
            host="$host.$hostExtension"
        fi
        echo "$host"

        # Issue command to kill processes for the specified user on the remote host
        echo "Issued command: ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host killall -u $netid"
        ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no "$netid@$host" "killall -u $netid"

        # Increment loop counter
        n=$((n + 1))
    done
)

echo "Cleanup complete"
