import java.io.Serializable;

public class NodeState implements Serializable {
    // Example fields
    private int someData;
    private String someOtherData;

    // Default constructor
    public NodeState() {
        this.someData = 0;
        this.someOtherData = "";
    }

    // Constructor with parameters
    public NodeState(int someData, String someOtherData) {
        this.someData = someData;
        this.someOtherData = someOtherData;
    }

    // Getters
    public int getSomeData() {
        return someData;
    }

    public String getSomeOtherData() {
        return someOtherData;
    }

    // Setters
    public void setSomeData(int someData) {
        this.someData = someData;
    }

    public void setSomeOtherData(String someOtherData) {
        this.someOtherData = someOtherData;
    }

    // You might want to override toString() for easy debugging
    @Override
    public String toString() {
        return "NodeState{" +
               "someData=" + someData +
               ", someOtherData='" + someOtherData + '\'' +
               '}';
    }
}
