import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Network {
    private Map<Integer, Node> nodes;
    private Map<Integer, Integer[]> connections; // Node ID to its neighbors
    private int minDelay; // Declare minDelay field
    private List<Operation> operations; // Declare operations field
    private boolean isProtocolActive;
    private static final int MAX_RETRY_ATTEMPTS = 3; // Maximum number of retry attempts
    private static final int RETRY_DELAY = 1000; // Delay between retries in milliseconds
    private static final int MAX_WAIT_TIME = 3000; // Maximum wait time in milliseconds

    public Network() {
        this.nodes = new HashMap<>();
        this.connections = new HashMap<>();
    }

    public synchronized void startProtocol() {
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0;

        while (isProtocolActive && elapsedTime < MAX_WAIT_TIME) {
            try {
                wait(MAX_WAIT_TIME - elapsedTime); // Wait for the remaining time
                elapsedTime = System.currentTimeMillis() - startTime;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("startProtocol interrupted.");
                return;
            }
        }

        if (isProtocolActive) {
            System.out.println("Protocol is still active after waiting. Unable to start a new protocol.");
            return;
        }

        isProtocolActive = true;
        // Start protocol
        System.out.println("Protocol started.");

        if (!isProtocolActive) {
            isProtocolActive = true;
            int sequenceNumber = generateSequenceNumber(); // Generate a sequence number
            for (Node node : nodes.values()) {
                node.initiateCheckpoint(sequenceNumber); // Start checkpoint with the generated sequence number
            }
            System.out.println("Protocol started with sequence number: " + sequenceNumber);
        }
    }
    
    public synchronized void endProtocol() {
        isProtocolActive = false;
        // Notify other nodes or perform cleanup
        notifyAll(); // Notify any waiting threads
        // Optionally, introduce a delay after ending the protocol
        try {
            Thread.sleep(minDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void startRecoveryProtocol() {
        if (!isProtocolActive) {
            System.out.println("No active protocol to recover from.");
            return;
        }
        for (Node node : nodes.values()) {
            node.initiateRecovery();
        }
        isProtocolActive = false;
    }

    public int generateSequenceNumber() {
        int aggregatedClockValue = 0;
        // Aggregate the vector clock values from all nodes
        for (Node node : nodes.values()) {
            Map<Integer, Integer> nodeVectorClock = node.getVectorClock();
            for (Integer clockValue : nodeVectorClock.values()) {
                aggregatedClockValue += clockValue;
            }
        }

        // Combine the aggregated value with the current time to ensure uniqueness
        long currentTime = System.currentTimeMillis();
        return (int) ((currentTime % Integer.MAX_VALUE) + aggregatedClockValue);
    }

    public Map<Integer, Node> getNodes() {
        return nodes;
    }
    public List<Operation> getOperations() {
        return operations;
    }

    public void addNode(Node node) {
        nodes.put(node.getNodeId(), node);
    }

    public void connectNodes(int nodeId1, int nodeId2) {
        connections.computeIfAbsent(nodeId1, k -> new Integer[0]);
        connections.computeIfAbsent(nodeId2, k -> new Integer[0]);
        connections.put(nodeId1, addToArray(connections.get(nodeId1), nodeId2));
        connections.put(nodeId2, addToArray(connections.get(nodeId2), nodeId1));
    }

    private Integer[] addToArray(Integer[] array, int value) {
        Integer[] newArray = new Integer[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = value;
        return newArray;
    }

    public Node getNode(int nodeId) {
        return nodes.get(nodeId);
    }

    public void setMinDelay(int minDelay) {
        this.minDelay = minDelay;
    }

    public void setOperations(List<Operation> operations) {
        this.operations = operations;
    }
    
    public void sendMessage(CheckpointMessage message) {
        Node sender = nodes.get(message.getSenderId());
        Node receiver = nodes.get(message.getReceiverId());
        String serializedMessage = serializeMessage(message);
    
        if (sender != null && receiver != null) {
            int attempt = 0;
            boolean messageSent = false;
    
            while (attempt < MAX_RETRY_ATTEMPTS && !messageSent) {
                try {
                    sender.sendMessage(receiver.getHostName(), receiver.getPort(), serializedMessage);
                    Thread.sleep(minDelay);
                    messageSent = true; // Message sent successfully
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Thread interrupted during sendMessage delay");
                    break;
                }
                attempt++;
                if (attempt < MAX_RETRY_ATTEMPTS && !messageSent) {
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        System.err.println("Thread interrupted during retry delay");
                        break;
                    }
                }
            }
    
            if (!messageSent) {
                System.err.println("Max retry attempts reached for sending message from Node " + message.getSenderId() + " to Node " + message.getReceiverId());
            }
        } else {
            System.err.println("Sender or receiver node not found for message: " + serializedMessage);
        }
    }

    private String serializeMessage(CheckpointMessage message) {
        // Convert the CheckpointMessage object to a String format
        // For simplicity, let's use a comma-separated format
        return message.getSenderId() + "," + message.getReceiverId() + "," + message.getSequenceNumber() + "," + message.getVectorClock().toString();
    }
}
