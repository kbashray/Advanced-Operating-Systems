import java.io.*;
import java.net.*;
import com.sun.nio.sctp.*;
import java.nio.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MyRCServer extends RoucairolCarvalho implements Runnable {
    public static final int MESSAGE_SIZE = 1000;
    public static boolean hasAllTerminated = false;

    public MyRCServer() {
    }

    public void startServer() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);

        try {
            SctpServerChannel sctpServerChannel = SctpServerChannel.open();
            int port = nodeMap.get(nodeId).hostPort;
            System.out.println("[INFO]\t[" + sTime() + "]\tNode Id " + nodeId + "\tPort : " + port);
            InetSocketAddress serverAddr = new InetSocketAddress(port);
            sctpServerChannel.bind(serverAddr);

            System.out.println("[INFO]\t[" + sTime() + "]\tNode Id " + nodeId + "\tSERVER STARTED");

            Thread.sleep(7000);
            minHeap = getPriorityQueue();

            while (true) {
                if (hasAllTerminated) {
                    if (!isTerminationSent) {
                        if (count == noOfCriticalSectionRequests) {
                            while (isInCriticalSection) ;
                            rCServer.sendTermination();
                        }
                    } else {
                        System.out.println("[INFO]\t[" + sTime() + "]\tTerminating Server at node " + nodeId);
                        System.exit(0);
                    }
                }

                SctpChannel sctpChannel = sctpServerChannel.accept();
                ByteBuffer receivedBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
                MessageInfo messageInfo = sctpChannel.receive(receivedBuffer, null, null);
                ByteArrayInputStream bin = new ByteArrayInputStream(receivedBuffer.array());
                ObjectInputStream oin = new ObjectInputStream(bin);
                Message messageObj = (Message) oin.readObject();

                handleReceivedMessage(messageObj);

                Thread.sleep(6000);
            }
        } catch (IOException | InterruptedException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    private void handleReceivedMessage(Message message) {
        int hostId;
        if (message.messageType == MessageType.REQUEST_KEY) {
            if (isInCriticalSection) {
                minHeap.add(message);
            } else if (requestForCriticalSection) {
                handleRequestInCriticalSection(message);
            } else {
                handleRequestOutsideCriticalSection(message);
            }
        } else if (message.messageType == MessageType.RESPONSE_KEY) {
            handleResponseKey(message);
        } else if (message.messageType == MessageType.RESPONSE_AND_REQUEST_KEY) {
            minHeap.add(message);
            handleResponseAndRequestKey(message);
        } else if (message.messageType == MessageType.TERMINATION_MESSAGE) {
            hostId = message.nodeInfo.hostId;
            nodeMap.get(hostId).isTerminated = true;
            hasAllTerminated = true;
            for (int i : nodeMap.keySet()) {
                if (!nodeMap.get(i).isTerminated) {
                    hasAllTerminated = false;
                }
            }
            if (hasAllTerminated) {
                handleAllTerminated(message);
            }
        }
    }

    private void handleRequestInCriticalSection(Message message) {
        int hostId;
        if (message.timeStamp.get() >= currentNodeCSEnterTimestamp.get()) {
            if (message.timeStamp.get() == currentNodeCSEnterTimestamp.get()) {
                if (nodeId < message.nodeInfo.hostId) {
                    minHeap.add(message);
                } else {
                    hostId = message.nodeInfo.hostId;
                    nodeMap.get(hostId).keyKnown = false;
                    startRCClient(message.nodeInfo, MessageType.RESPONSE_AND_REQUEST_KEY);
                }
            } else {
                minHeap.add(message);
            }
        } else {
            hostId = message.nodeInfo.hostId;
            nodeMap.get(hostId).keyKnown = false;
            startRCClient(message.nodeInfo, MessageType.RESPONSE_AND_REQUEST_KEY);
        }
    }

    private void handleRequestOutsideCriticalSection(Message message) {
        int hostId = message.nodeInfo.hostId;
        nodeMap.get(hostId).keyKnown = false;
        startRCClient(message.nodeInfo, MessageType.RESPONSE_KEY);
    }

    private void handleResponseKey(Message message) {
        int hostId = message.nodeInfo.hostId;
        nodeMap.get(hostId).keyKnown = true;
        nodeMap.get(hostId).isRequested = false;

        if (!isAllNodeKeysKnown()) {
            handleNotAllNodeKeysKnown();
        }
    }

    private void handleNotAllNodeKeysKnown() {
    System.out.println("Not all nodes have known keys. Handling the case...");
    }


    private void handleResponseAndRequestKey(Message message) {
        int hostId = message.nodeInfo.hostId;
        nodeMap.get(hostId).keyKnown = true;
        nodeMap.get(hostId).isRequested = false;
    }

    private void handleAllTerminated(Message message) {
        if (!isTerminationSent) {
            if (count == noOfCriticalSectionRequests) {
                while (isInCriticalSection) ;
                rCServer.sendTermination();
            }
        } else {
            System.out.println("[INFO]\t[" + sTime() + "]\tTerminating Server at node " + nodeId);
            System.exit(0);
        }
    }

    private void requestAllKeys() {
        Host host;
        for (int nId : nodeMap.keySet()) {
            host = nodeMap.get(nId);
            if (!host.keyKnown && nodeId != host.hostId && nodeMap.get(host.hostId).isRequested != true) {
                startRCClient(host, MessageType.REQUEST_KEY);
                nodeMap.get(host.hostId).isRequested = true;
            }
        }
    }

    private void sendTermination() {
        Host host = null;
        nodeMap.get(nodeId).isTerminated = true;
        Thread a[] = new Thread[nodeMap.keySet().size()];
        int index = 0;
        for (int nId : nodeMap.keySet()) {
            if (nId != nodeId) {
                host = nodeMap.get(nId);
                RCClient rCClient;
                Message message;
                message = new Message(currentNodeCSEnterTimestamp, MessageType.TERMINATION_MESSAGE, nodeMap.get(nodeId));
                rCClient = new RCClient(host, message);
                a[index] = new Thread(rCClient);
                a[index].start();
                index++;
            }
        }
        Boolean isAllDone = true;
        for (int i : nodeMap.keySet()) {
            if (i != nodeId) {
                if (!nodeMap.get(i).isTerminated) {
                    isAllDone = false;
                }
            }
        }
        if (isAllDone) {
            for (int j = 0; j < a.length; j++) {
                while (a[j] != null && a[j].isAlive()) ;
            }
            System.out.println("[INFO]\t[" + sTime() + "]\tTerminating Server at node " + nodeId);
            System.exit(0);
        }
        isTerminationSent = true;
    }

    private boolean isAllNodeKeysKnown() {
        Host host;
        for (int nId : nodeMap.keySet()) {
            host = nodeMap.get(nId);
            if (!host.keyKnown && host.hostId != nodeId) {
                return false;
            }
        }
        return true;
    }

    private String sTime() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        return timeStamp;
    }

    private String byteToString(ByteBuffer byteBuffer) {
        byteBuffer.position(0);
        byteBuffer.limit(MESSAGE_SIZE);
        byte[] bufArr = new byte[byteBuffer.remaining()];
        byteBuffer.get(bufArr);
        return new String(bufArr);
    }

    private void writeOutputToFile() throws FileNotFoundException, UnsupportedEncodingException {
        String fileName = "node" + nodeId + ".txt";
        PrintWriter writer = new PrintWriter(fileName, "UTF-8");
        writer.println("\n Discovered all nodes in this network");
        writer.println("Total No. of Nodes : " + nodeMap.size());
        writer.println("\n LIST OF NODES");
        for (int n : nodeMap.keySet()) {
            Host hNode = nodeMap.get(n);
            writer.println(hNode.hostId + "    " + hNode.hostName + "     " + hNode.hostPort);
        }
        writer.close();
    }

    private void startRCClient(Host host, MessageType sMessageType) {
        if (host.hostId != nodeId) {
            if (sMessageType == MessageType.RESPONSE_AND_REQUEST_KEY && nodeMap.get(host.hostId).isRequested == true) {
                sMessageType = MessageType.RESPONSE_KEY;
            }
            if (sMessageType == MessageType.REQUEST_KEY && (nodeMap.get(host.hostId).isRequested == true || nodeMap.get(host.hostId).keyKnown == true)) {
                return;
            }
            if (sMessageType == MessageType.REQUEST_KEY) {
                nodeMap.get(host.hostId).isRequested = true;
            }
            RCClient rCClient;
            Message message;
            message = new Message(currentNodeCSEnterTimestamp, sMessageType, nodeMap.get(nodeId));
            rCClient = new RCClient(host, message);
            new Thread(rCClient).start();
        }
    }

    private void startRCClients(PriorityBlockingQueue<Message> minHeap, MessageType sMessageType) {
        int size = minHeap.size();
        int nNumOfThreads = size;
        Thread[] tThreads = new Thread[nNumOfThreads];
        RCClient rCClient;
        Message message;
        int i = 0;
        currentNodeCSEnterTimestamp.incrementAndGet();

        while (minHeap.size() > 0) {
            Message m = minHeap.poll();
            if (m != null) {
                Host host = m.nodeInfo;
                if (nodeId != host.hostId) {
                    if (sMessageType == MessageType.REQUEST_KEY && nodeMap.get(host.hostId).keyKnown == true) {
                        return;
                    }
                    if (sMessageType == MessageType.RESPONSE_KEY || sMessageType == MessageType.RESPONSE_AND_REQUEST_KEY) {
                        nodeMap.get(host.hostId).keyKnown = false;
                        if (sMessageType == MessageType.RESPONSE_AND_REQUEST_KEY) {
                            nodeMap.get(host.hostId).isRequested = true;
                        }
                    }
                    if (sMessageType == MessageType.REQUEST_KEY) {
                        nodeMap.get(host.hostId).isRequested = true;
                    }
                    message = new Message(currentNodeCSEnterTimestamp, sMessageType, nodeMap.get(nodeId));
                    rCClient = new RCClient(host, message);
                    tThreads[i] = new Thread(rCClient);
                    tThreads[i].start();
                    i++;
                }
            }
        }
    }

    // Implement the run method (entry point for the thread)
    public void run() {
        startServer();
    }

    public static void main(String args[]) {
        MyRCServer myRCServer = new MyRCServer();
        myRCServer.startServer();
    }
}
