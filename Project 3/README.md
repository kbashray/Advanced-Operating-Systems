Implement Koo and Toueg's checkpointing and recovery protocol. Develop a simple application to
demonstrate the working of the protocol.
You should design your program to allow any node to initiate an instance of checkpointing or
recovery. However, you can assume that at most one instance of checkpointing/recovery protocol
will be in progress at any time. For example, if node 2 has initiated an instance of checkpointing
protocol, then no other node can initiate an instance of checkpointing or recovery protocol until
the instance initiated by node 2 has terminated.
You will be provided with a sequence of checkpointing/recovery operations you have to simulate
in a conguration le. The sequence will be given by a list of tuples; the rst entry in a tuple
will denote a node identier and the second entry will denote an operation type (checkpointing
or recovery). As an example, if the list of tuples is (2,c), (1,r), (3,c), (2,c) then your program
should execute an instance of checkpointing protocol initiated by node 2, followed by an instance
of recovery protocol initiated by node 1, and so on.
Avoiding Concurrent Instances: To ensure that only one instance of checkpointing/recovery
protocol is in progress at any time, use the following approach. Once the current instance of
checkpointing/recovery protocol terminates, its initiator informs the initiator of the next instance
of the checkpointing/recovery protocol (of the termination of the current instance) using simple

ooding. The latter then waits for minDelay time units before initiating the protocol. In the
previous example, node 2 should inform node 1 which in turn should inform node 3 and so on.
Contents of a Checkpoint: Each instance of checkpointing protocol will have a sequence num-
ber associated with it. A node, when taking a checkpoint, stores (a) the sequence number of
the checkpointing protocol, (b) the current value of its vector clock (you will need to implement
vector clock protocol for this), and (c) any other information you may deem to be necessary for
application's recovery.
Testing: Show that your checkpointing and recovery protocols work correctly. Specically, for
the checkpointing protocol, you have to show that the set of last permanent checkpoints form a
consistent global state. For the recovery protocol, you have to show that the protocol rolls back
the system to a consistent global state.
