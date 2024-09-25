import java.io.IOException;
public class CheckpointingSimulation {
    public static void main(String[] args) {
        try {
            Network network = ConfigParser.parseConfigurationFile("config.txt");
            // Execute the operations based on the parsed configuration
            for (Node node : network.getNodes().values()) {
                node.startServer();
            }

            // Execute the operations
            for (Operation operation : network.getOperations()) {
                Node node = network.getNode(operation.getNodeId());
                if (node != null) {
                    if (operation.getOperationType() == 'c') {
                        // Initiate checkpointing
                        node.initiateCheckpoint(2);
                    } else if (operation.getOperationType() == 'r') {
                        // Initiate recovery
                        node.initiateRecovery();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
