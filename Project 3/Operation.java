public class Operation {
    private char operationType; // 'c' for checkpoint, 'r' for recovery
    private int nodeId;

    public Operation(char operationType, int nodeId) {
        this.operationType = operationType;
        this.nodeId = nodeId;
    }

    public char getOperationType() {
        return operationType;
    }

    public int getNodeId() {
        return nodeId;
    }
}
