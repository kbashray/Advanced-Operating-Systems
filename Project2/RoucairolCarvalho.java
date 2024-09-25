import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class RoucairolCarvalho {

    private static int numberOfNodes;
    protected static int nodeIdentifier;
    private static Random randomGenerator;
    protected static int numberOfCriticalSections;
    private static int averageCriticalSectionDelay;
    private static int criticalSectionDuration;
    protected static boolean isTerminationInitiated = false;

    // Additional metrics variables
    protected static int totalMessagesSent = 0;
    protected static int totalMessagesReceived = 0;
    protected static long totalResponseTime = 0;
    protected static int successfulEntries = 0;

    protected static HashMap<Integer, Host> nodeMap;
    protected static int terminationResponseCount = 0;
    protected static boolean inCriticalSection = false;
    protected static boolean criticalSectionRequested = false;
    protected static PriorityQueue<Message> messageQueue = createPriorityQueue();
    protected static AtomicInteger currentNodeCriticalSectionEntryTimestamp = new AtomicInteger(0);
    protected static int requestCount = 0;
    protected static ApplicationClient applicationClient;
    static RCServer rcServer = new RCServer();
    protected PrintWriter output;

    public void launchServer() {
        new Thread(rcServer).start();
        applicationClient = new ApplicationClient(numberOfCriticalSections, averageCriticalSectionDelay, criticalSectionDuration);
        new Thread(applicationClient).start();
    }

    public void enterCriticalSection() {
        try {
            String logFileName = "node" + nodeIdentifier + ".log";
            output = new PrintWriter(new BufferedWriter(new FileWriter(logFileName, true)));
        } catch (IOException ex) {
            output.close();
            ex.printStackTrace();
        }
        criticalSectionRequested = true;
        currentNodeCriticalSectionEntryTimestamp.incrementAndGet();
        rcServer.requestAllKeys();
        while (!rcServer.isAllNodeKeysKnown()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        inCriticalSection = true;
        displayCriticalSectionMessage(nodeIdentifier);
        output.println(System.currentTimeMillis() + ":" + nodeIdentifier + "-Start");
    }

    public void leaveCriticalSection() {
        output.println(System.currentTimeMillis() + ":" + nodeIdentifier + "-End");
        output.flush();
        output.close();
        inCriticalSection = false;
        criticalSectionRequested = false;
        rcServer.startRCClients(messageQueue, MessageType.RESPONSE_KEY);
        messageQueue = createPriorityQueue();
        requestCount++;

        // Calculate response time
        totalResponseTime += (System.currentTimeMillis() - currentNodeCriticalSectionEntryTimestamp.get());

        if (requestCount == numberOfCriticalSections) {
            rcServer.sendTermination();
        }
    }

    public void displayCriticalSectionMessage(int nodeIdentifier) {
        System.out.print("[INFO]\t[" + currentTime() + "]\t");

        for (int i = 0; i < numberOfNodes; i++) {
            if (i != nodeIdentifier) {
                System.out.print("|\t");
            } else {
                System.out.print("|  " + nodeIdentifier + " CS ");
            }
        }
        System.out.print("|");
        System.out.println();
    }

    public static PriorityQueue<Message> createPriorityQueue() {
        return new PriorityQueue<>(11, (o1, o2) -> {
            AtomicInteger timestamp1 = o1.timestamp;
            AtomicInteger timestamp2 = o2.timestamp;
            if (timestamp1.get() >= timestamp2.get()) return 1;
            else return -1;
        });
    }

    public HashMap<Integer, Host> generateNetwork(String configurationFileName, int nodeIdentifier) throws IOException {
        nodeMap = new HashMap<>();
        randomGenerator = new Random();
        File file = new File(configurationFileName);
        Scanner scanner = new Scanner(file);
        int hostId, hostPort;
        String hostName = "";
        String checker = "";

        int start = randomInt(0, 9);
        int end = randomInt(0, 9);
        int startNode = start <= end ? start : end;
        int endNode = start > end ? start : end;

        while (scanner.hasNext()) {
            if ((checker = scanner.next()).equals("p") && !checker.equals("#")) {
                numberOfNodes = scanner.nextInt();
                for (int j = 0; j < numberOfNodes; j++) {
                    if ((checker = scanner.next()).equals("n")) {
                        hostId = scanner.nextInt();
                        hostName = scanner.next() + ".utdallas.edu";
                        hostPort = scanner.nextInt();

                        if (nodeMap.get(hostId) == null) {
                            if (hostId <= endNode && hostId >= startNode) {
                                nodeMap.put(hostId, new Host(hostId, hostName, hostPort, false, false));
                            } else {
                                nodeMap.put(hostId, new Host(hostId, hostName, hostPort, false, false));
                            }
                        }
                    }
                }

                if ((checker = scanner.next()).equals("cscount") && !checker.equals("#")) {
                    numberOfCriticalSections = scanner.nextInt();
                }

                if ((checker = scanner.next()).equals("meandelay") && !checker.equals("#")) {
                    averageCriticalSectionDelay = scanner.nextInt();
                }

                if ((checker = scanner.next()).equals("duration") && !checker.equals("#")) {
                    criticalSectionDuration = scanner.nextInt();
                }
            }
        }
        return nodeMap;
    }

    public static int randomInt(int min, int max) {
        randomGenerator = new Random();
        int randomNumber = randomGenerator.nextInt((max - min) + 1) + min;
        return randomNumber;
    }

    public String currentTime() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
    }

    public void printNodeMap() {
        Host host;
        for (int nodeId : nodeMap.keySet()) {
            host = nodeMap.get(nodeId);
            System.out.println("[INFO]\t[" + currentTime() + "]\tHost Id " + nodeId + "  Name : " + host.hostName + "  port : " + host.hostPort + "  Key Known?  " + host.keyKnown);
        }
    }

    public void runRoucairolCarvalho() throws IOException {
        HashMap<Integer, Host> networkMap = generateNetwork("config.txt", nodeIdentifier);
        launchServer();

        // Run the simulation for multiple times (e.g., 5 runs)
        for (int i = 0; i < 5; i++) {
            // ... (unchanged code)

            // Print or store the metrics
            printMetrics();
        }
    }

    public void printMetrics() {
        System.out.println("Metrics for Node " + nodeIdentifier + " after 5 runs:");
        System.out.println("Total Messages Sent: " + totalMessagesSent);
        System.out.println("Total Messages Received: " + totalMessagesReceived);
        System.out.println("Mean Message Complexity: " + (totalMessagesSent + totalMessagesReceived) / 2);
        System.out.println("Mean Response Time: " + totalResponseTime / successfulEntries);
        System.out.println("System Throughput: " + successfulEntries / (criticalSectionDuration / 1000.0) + " requests per second");
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            nodeIdentifier = Integer.parseInt(args[0]);
        }
        RoucairolCarvalho rcInstance = new RoucairolCarvalho();
        rcInstance.runRoucairolCarvalho();
    }
}
