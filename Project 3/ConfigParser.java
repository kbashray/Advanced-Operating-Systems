import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigParser {

    public static Network parseConfigurationFile(String filePath) throws IOException {
        Network network = new Network();
        int numberOfNodes = 0;
        int minDelay = 0;
        List<List<Integer>> neighbors = new ArrayList<>();
        List<Operation> operations = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean readingNeighbors = false;
            int nodeCounter = 0;

            while ((line = reader.readLine()) != null) {
                line = line.split("#")[0].trim(); // Remove comments and trim whitespace
                if (line.isEmpty()) continue;

                if (!readingNeighbors) {
                    String[] parts = line.split("\\s+");
                    if (parts.length == 2) {
                        // Read global parameters
                        numberOfNodes = Integer.parseInt(parts[0]);
                        minDelay = Integer.parseInt(parts[1]);
                        for (int i = 0; i < numberOfNodes; i++) {
                            neighbors.add(new ArrayList<>());
                        }
                    } else if (parts.length == 3) {
                        // Read node details
                        int nodeId = Integer.parseInt(parts[0]);
                        String hostName = parts[1];
                        int port = Integer.parseInt(parts[2]);
                        network.addNode(new Node(nodeId, hostName, port,network));
                    } else if (parts.length == 1) {
                        // Switch to reading neighbors
                        readingNeighbors = true;
                    }
                } else {
                    // Read neighbors or operations
                    if (nodeCounter < numberOfNodes) {
                        String[] parts = line.split("\\s+");
                        for (String part : parts) {
                            neighbors.get(nodeCounter).add(Integer.parseInt(part));
                        }
                        nodeCounter++;
                    } else {
                        // Read operations
                        String[] parts = line.replaceAll("[()\\s]", "").split(",");
                        if (parts.length == 2) {
                            char operationType = parts[0].charAt(0);
                            int nodeId = Integer.parseInt(parts[1]);
                            operations.add(new Operation(operationType, nodeId));
                        }
                    }
                }
            }
        }

        // Add neighbors to nodes
        for (int i = 0; i < numberOfNodes; i++) {
            Node node = network.getNode(i);
            if (node != null) {
                for (int neighborId : neighbors.get(i)) {
                    Node neighbor = network.getNode(neighborId);
                    if (neighbor != null) {
                        node.addNeighbor(neighbor);
                    }
                }
            }
        }

        // Set global parameters and operations in the network
        network.setMinDelay(minDelay);
        network.setOperations(operations);

        return network;
    }
}
