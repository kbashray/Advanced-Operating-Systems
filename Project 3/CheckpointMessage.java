import java.util.Map;

public class CheckpointMessage {
    private int senderId;
    private int receiverId;
    private int sequenceNumber;
    private Map<Integer, Integer> vectorClock;

    public CheckpointMessage(int senderId, int receiverId, int sequenceNumber, Map<Integer, Integer> vectorClock) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.sequenceNumber = sequenceNumber;
        this.vectorClock = vectorClock;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public Map<Integer, Integer> getVectorClock() {
        return vectorClock;
    }
}
