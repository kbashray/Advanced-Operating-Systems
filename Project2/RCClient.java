import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

public class RCClient implements Runnable {

    public static final int MESSAGE_SIZE = 1000;

    private Host hostToBeRequested;
    private Message messageForHost;

    public RCClient(Host hostToBeRequested, Message messageForHost) {
        this.hostToBeRequested = hostToBeRequested;
        this.messageForHost = messageForHost;
    }

    public void go() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
        try {
            Thread.sleep(1000);

            // Connect to the server
            SocketAddress socketAddress = new InetSocketAddress(hostToBeRequested.hostName, hostToBeRequested.hostPort);
            SctpChannel sctpChannel = SctpChannel.open();
            sctpChannel.connect(socketAddress);

            // Prepare the message for sending
            MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oout;
            try {
                oout = new ObjectOutputStream(bout);
                oout.writeObject(messageForHost);
                byteBuffer.put(bout.toByteArray());
                byteBuffer.flip();

                // Send the message
                sctpChannel.send(byteBuffer, messageInfo);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                byteBuffer.clear();
            }

        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public void run() {
        go();
    }

    public static void main(String args[]) {
        if (args.length < 1) {
            System.out.println("Usage: java RCClient <config_file_path>");
            return;
        }

        try {
            // Read configuration from the provided file
            String configFilePath = args[0];
            String configContent = new String(Files.readAllBytes(Paths.get(configFilePath)));

            // Split the content into lines
            String[] lines = configContent.split("\\r?\\n");

            // Parse the first line for global configuration
            String[] globalConfigTokens = lines[0].split("\\s+");
            int numberOfNodes = Integer.parseInt(globalConfigTokens[0]);
            int interRequestDelay = Integer.parseInt(globalConfigTokens[1]);
            int csExecutionTime = Integer.parseInt(globalConfigTokens[2]);
            int numberOfRequests = Integer.parseInt(globalConfigTokens[3]);

            // Parse each node's configuration
            for (int i = 1; i <= numberOfNodes; i++) {
                String[] nodeConfigTokens = lines[i].split("\\s+");
                int nodeId = Integer.parseInt(nodeConfigTokens[0]);
                String hostName = nodeConfigTokens[1];
                int port = Integer.parseInt(nodeConfigTokens[2]);

                // Create Host and Message instances
                Host requestedHost = new Host(nodeId, hostName, port, false, false);
                AtomicInteger timeStamp = new AtomicInteger(/* Set appropriate timestamp value */);
                MessageType messageType = /* Set appropriate message type */;
                Message messageForHost = new Message(timeStamp, messageType, requestedHost);

                // Create RCClient instance and start the client
                RCClient rcClient = new RCClient(requestedHost, messageForHost);
                new Thread(rcClient).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
