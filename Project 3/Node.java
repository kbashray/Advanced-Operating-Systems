import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.*;

public class Node {
    private NodeState state;
    private int nodeId;
    private String hostName;
    private int port;
    private Map<Integer, Integer> vectorClock; // Vector clock for each node
    private int sequenceNumber; // Sequence number for the checkpoint
    private boolean isCheckpointing; // Flag to indicate if checkpointing is in progress
    private Set<Node> neighbors; // Neighbors of this node
    private Network network; // Reference to the network for communication
    private static final int MAX_DELAY = 1000;

    public Node(int nodeId, String hostName, int port, Network network) {
        this.nodeId = nodeId;
        this.hostName = hostName;
        this.port = port;
        this.vectorClock = new HashMap<>();
        this.sequenceNumber = 0;
        this.isCheckpointing = false;
        this.neighbors = new HashSet<>();
        this.network = network;
        this.state = new NodeState();
    }

    public void addNeighbor(Node neighbor) {
        neighbors.add(neighbor);
    }

    public void initiateCheckpoint(int sequenceNumber) {
        if (isCheckpointing) {
            System.out.println("Checkpointing already in progress at Node " + nodeId);
            return;
        }

        this.sequenceNumber = sequenceNumber;
        isCheckpointing = true;
        saveCurrentState();
        sendCheckpointMessages();

        // Update vector clock for the checkpoint
        vectorClock.put(nodeId, vectorClock.getOrDefault(nodeId, 0) + 1);

        System.out.println("Node " + nodeId + " started checkpointing with sequence number " + sequenceNumber);

        // Send checkpoint request to all neighbors
        for (Node neighbor : neighbors) {
            network.sendMessage(new CheckpointMessage(nodeId, neighbor.getNodeId(), sequenceNumber, new HashMap<>(vectorClock)));
        }
        
    }
    private boolean shouldTakeCheckpoint(CheckpointMessage message) {
        // Check if the received sequence number is greater than the local sequence number
        if (message.getSequenceNumber() > this.sequenceNumber) {
        return true;
        }

        // Check if the received vector clock is ahead of the local vector clock
        Map<Integer, Integer> receivedVectorClock = message.getVectorClock();
        for (Map.Entry<Integer, Integer> entry : receivedVectorClock.entrySet()) {
            Integer localTime = this.vectorClock.getOrDefault(entry.getKey(), 0);
            if (entry.getValue() > localTime) {
                return true;
            }
        }

        // If neither condition is met, no need to take a checkpoint
        return false;
    }


    private void sendCheckpointMessages() {
        for (Node neighbor : neighbors) {
            CheckpointMessage message = new CheckpointMessage(nodeId, neighbor.getNodeId(), sequenceNumber, new HashMap<>(vectorClock));
            network.sendMessage(message);
        }
    }


    public void receiveCheckpointMessage(CheckpointMessage message) {
        if (shouldTakeCheckpoint(message)) {
            this.sequenceNumber = message.getSequenceNumber();
            isCheckpointing = true;
            resetForNewCheckpoint();
            propagateCheckpointMessage(message);
        }

        for (Map.Entry<Integer, Integer> entry : message.getVectorClock().entrySet()) {
            updateVectorClock(entry.getKey(), entry.getValue());
        }
    }

    public void initiateRecovery() {
        recoverToSavedState();
        System.out.println("Node " + nodeId + " started recovery process.");
    }

    private void recoverToSavedState() {
        try {
            FileInputStream fileIn = new FileInputStream("node_state_" + nodeId + ".ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
    
            // Deserialize the saved state
            NodeState savedState = (NodeState) in.readObject();
    
            // Restore the state
            this.state = savedState;
    
            in.close();
            fileIn.close();
            System.out.println("Node " + nodeId + " recovered to saved state at sequence number " + sequenceNumber);
        } catch (IOException i) {
            System.err.println("IOException during state recovery: " + i.getMessage());
        } catch (ClassNotFoundException c) {
            System.err.println("NodeState class not found during recovery: " + c.getMessage());
        }
    }

    public void updateVectorClock(int nodeId, int timestamp) {
        this.vectorClock.put(nodeId, Math.max(this.vectorClock.getOrDefault(nodeId, 0), timestamp));
    }

    public void updateState(int someData, String someOtherData) {
        state.setSomeData(someData);
        state.setSomeOtherData(someOtherData);
    }
    private void propagateCheckpointMessage(CheckpointMessage message) {
        // If the protocol requires, propagate the checkpoint message to neighbors
        for (Node neighbor : neighbors) {
            if (neighbor.getNodeId() != message.getSenderId()) {
                // Create a new message or modify the existing one as per protocol requirements
                CheckpointMessage newMessage = new CheckpointMessage(this.nodeId, neighbor.getNodeId(), this.sequenceNumber, new HashMap<>(this.vectorClock));
                network.sendMessage(newMessage);
            }
        }
    }
    private void resetForNewCheckpoint() {
        saveCurrentState();
        updateVectorClockForCheckpoint();
        coordinateCheckpointWithNeighbors();
        // Reset any additional state
    }

    private void coordinateCheckpointWithNeighbors() {
        // Assuming you have a field 'neighbors' which is a collection of Node objects
        CountDownLatch latch = new CountDownLatch(neighbors.size());

        for (Node neighbor : neighbors) {
            new Thread(() -> {
                sendCheckpointStartMessage(neighbor);
                latch.countDown();
            }).start();
        }

        try {
            // Wait for all acknowledgments with a timeout
            boolean allAcknowledged = latch.await(MAX_DELAY, TimeUnit.MILLISECONDS);
            if (!allAcknowledged) {
                System.out.println("Timeout: Not all neighbors acknowledged the checkpoint start.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Interrupted while waiting for acknowledgments.");
        }
    }

    private void sendCheckpointStartMessage(Node neighbor) {
        // This could involve serializing a message object and sending it over the network
        CheckpointMessage message = new CheckpointMessage(this.nodeId, neighbor.getNodeId(), sequenceNumber, new HashMap<>(vectorClock));
        network.sendMessage(message);
    }

    private void updateVectorClockForCheckpoint() {
        // Increment the vector clock for the checkpoint event
        vectorClock.put(nodeId, vectorClock.getOrDefault(nodeId, 0) + 1);
    }

    private void saveCurrentState() {
        try {
            FileOutputStream fileOut = new FileOutputStream("node_state_" + nodeId + ".ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);

            // Assuming 'state' is already updated with the current state of the node
            out.writeObject(state); // Serialize the state object

            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in node_state_" + nodeId + ".ser");
        } catch (IOException i) {
            System.err.println("IOException during state saving: " + i.getMessage());
        }
    }

    // Getters and setters
    public int getNodeId() {
        return nodeId;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public Map<Integer, Integer> getVectorClock() {
        return vectorClock;
    }

    public void setVectorClock(Map<Integer, Integer> vectorClock) {
        this.vectorClock = vectorClock;
    }
    public void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    try (Socket clientSocket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            // Process the message
                            processMessage(inputLine);
                        }
                    } catch (IOException e) {
                        System.out.println("Exception caught when trying to listen on port " + port + " or listening for a connection");
                        System.out.println(e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.out.println("Could not listen on port: " + port);
                System.out.println(e.getMessage());
            }
        }).start();
    }

    private void processMessage(String message) {
        System.out.println("Message received at Node " + nodeId + ": " + message);
    
        // Parse the message
        String[] parts = message.split(",");
        if (parts.length < 4) {
            System.err.println("Invalid message format received.");
            return;
        }
    
        try {
            int senderId = Integer.parseInt(parts[0]);
            int receiverId = Integer.parseInt(parts[1]);
            int sequenceNumber = Integer.parseInt(parts[2]);
            Map<Integer, Integer> vectorClock = parseVectorClock(parts[3]);
    
            if (receiverId != this.nodeId) {
                System.err.println("Message received for a different node: " + receiverId);
                return;
            }
    
            // Create a CheckpointMessage object from the parsed data
            CheckpointMessage receivedMessage = new CheckpointMessage(senderId, receiverId, sequenceNumber, vectorClock);
    
            receiveCheckpointMessage(receivedMessage);
    
        } catch (NumberFormatException e) {
            System.err.println("Error parsing message: " + e.getMessage());
        }
    }
    
    private Map<Integer, Integer> parseVectorClock(String vectorClockStr) {
        Map<Integer, Integer> vectorClock = new HashMap<>();
        String trimmed = vectorClockStr.replaceAll("[{}]", "");
        String[] entries = trimmed.split(", ");
        for (String entry : entries) {
            String[] keyValue = entry.split("=");
            int key = Integer.parseInt(keyValue[0]);
            int value = Integer.parseInt(keyValue[1]);
            vectorClock.put(key, value);
        }
        return vectorClock;
    }

    public void sendMessage(String host, int port, String message) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(message);
        } catch (IOException e) {
            System.out.println("Couldn't get I/O for the connection to " + host);
            System.out.println(e.getMessage());
        }
    }
}