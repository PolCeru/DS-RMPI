package it.polimi.ds.communication;

import com.google.gson.Gson;
import it.polimi.ds.reliability.ReliabilityMessage;
import it.polimi.ds.utils.MessageGsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Base layer of the protocol stack, allows to create a broadcast network of devices using a mesh topology of
 * point-to-point connections.
 * <p>This layer is responsible for:
 * <ul>
 *     <li> discovering other devices on the network</li>
 *     <li> establishing a connection with other devices </li>
 *     <li> create {@link ClientHandler} for each connected device </li>
 * </ul></p>
 */
public class CommunicationLayer {

    /**
     * Default broadcast address
     */
    private static final String BROADCAST_ADDR = "255.255.255.255";
    /**
     * Default communication port
     */
    private static final int DEFAULT_PORT = 4445;

    /**
     * Default time interval between new discovery message is sent
     */
    private static final int BROADCAST_INTERVAL = 10000;


    /**
     * Identify this machine uniquely
     */
    private final UUID uuid = UUID.randomUUID();

    /**
     * Random number used by the protocol to decide which device is the master and should start the connection
     */
    private final int random = new Random().nextInt();

    /**
     * Show if this piece is still locking for a device networks
     */
    private volatile boolean isConnected = false;

    /**
     * Serializer of basic messages
     */
    static final Gson gson = new MessageGsonBuilder().registerMessageAdapter()
            .registerLocalDateTimeAdapter()
            .create();

    /**
     * List of all connected clients
     */
    private final Map<UUID, ClientHandler> connectedClients = new HashMap<>();

    /**
     * Buffer of messages to be sent to the upper Reliability layer
     */
    private final BlockingQueue<BasicMessage> upBuffer = new LinkedBlockingQueue<>();

    /**
     * Port used for communication
     */
    private final int port;

    /**
     * Broadcast interval between discovery messages
     */
    private final int broadcastInterval;

    private CommunicationLayer(int port, int broadcastInterval) {
        this.port = port;
        this.broadcastInterval = broadcastInterval;
        init();
    }

    /**
     * Construct the protocol with default configuration
     */
    public static CommunicationLayer defaultConfiguration() {
        return new CommunicationLayer(DEFAULT_PORT, BROADCAST_INTERVAL);
    }

    /**
     * Construct the protocol with personalized network configurations
     *
     * @param port              the port used for communication
     * @param broadcastInterval the interval between discovery messages
     */
    public static CommunicationLayer customConfiguration(int port, int broadcastInterval) {
        return new CommunicationLayer(port, broadcastInterval);
    }

    /**
     * Serialize and convert a message into bytes using UTF-8 encoding
     *
     * @param message the message to convert
     * @return the byte encoding of the json serialization
     */
    private static byte[] encodeMessage(BasicMessage message) {
        return gson.toJson(message).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Deserialize and convert a message from bytes, assuming {@code UTF-8} encoding
     **/
    private static BasicMessage decodeMessage(byte[] payload, int length) {
        return gson.fromJson(new String(payload, 0, length, StandardCharsets.UTF_8), DiscoveryMessage.class);
    }

    /**
     * Start the required thread to allow the discovery of other devices on the network and the listeners to allow
     * connections
     */
    protected void init() {
        new Thread(this::startDiscoveryListener, "Discovery Listener").start();
        new Thread(this::startServerListener, "Server Listener").start();
        Thread discoverySender = new Thread(this::startDiscoverySender, "Discovery sender");
        discoverySender.start();
        try {
            discoverySender.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Start a thread that send discovery messages periodically on the UDP broadcast network; the period is defined by
     * the initial configuration of this protocol
     */
    private void startDiscoverySender() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);

            while (!isConnected) {
                DiscoveryMessage message = new DiscoveryMessage(LocalDateTime.now(), uuid, port, random);
                byte[] sendData = encodeMessage(message);

                DatagramPacket packet = new DatagramPacket(sendData, sendData.length,
                        InetAddress.getByName(BROADCAST_ADDR), port);

                socket.send(packet);
                System.out.println("Broadcast message sent: " + message);

                Thread.sleep(broadcastInterval);
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Start a thread that listen for discovery messages sent to a TCP socket; when a new {@link DiscoveryMessage} is
     * received from this socket a connection with the associated device is established.
     */
    private void startServerListener() {
        InputStream inputStream;
        byte[] buffer;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                inputStream = socket.getInputStream();
                buffer = inputStream.readNBytes(1024);
                DiscoveryMessage message = (DiscoveryMessage) decodeMessage(buffer, buffer.length);
                System.out.println("Received connection message from " + socket.getInetAddress()
                        .getHostAddress() + " - " + message.getRandom());
                //TODO fix the random check and disable it when already connected
                if (message.random <= this.random) {
                    addClient(message.getSenderUID(), socket);
                } else System.err.println(
                        "My random is " + this.random + " but i received a message from " + socket.getInetAddress()
                                .getHostAddress() + " with random " + message.random);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Start a thread that listen for discovery messages on the UDP broadcast network and when a new device is found it
     * tries to establish a connection creating a TCP socket with it. This connection is also used to deliver a
     * {@link DiscoveryMessage} to the new device to allow it to connect to this device.
     */
    private void startDiscoveryListener() {
        final int bufferSize = 1024;
        try (DatagramSocket datagramSocket = new DatagramSocket(port)) {
            byte[] receiveData = new byte[bufferSize];

            while (true) {
                DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                datagramSocket.receive(packet);

                DiscoveryMessage message = (DiscoveryMessage) decodeMessage(packet.getData(), packet.getLength());
                if (message.random > this.random) {
                    InetAddress address = packet.getAddress();
                    try (Socket socket = new Socket(address, port)) {
                        socket.getOutputStream()
                                .write(encodeMessage(new DiscoveryMessage(LocalDateTime.now(), uuid, port, random)));
                        addClient(message.getSenderUID(), socket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a client handler for the corresponding real device (identified by the senderUID) and start the main
     * thread of the client handler
     * @param senderUID the senderUID of the device to connect to
     * @param socket the socket used to communicate with the device
     */
    synchronized private void addClient(UUID senderUID, Socket socket) {
        ClientHandler clientHandler = new ClientHandler(senderUID, socket, this);
        connectedClients.put(senderUID, clientHandler);
        new Thread(clientHandler).start();
        if (!isConnected) isConnected = !isConnected;
        System.out.println("Successfully connected with device " + socket.getInetAddress().getHostAddress());
    }

    /**
     * Send a message to a specific client, used to send an ACK message after a DATA message or to resend a DATA message
     * to a specific client.
     * @param clientID the client to which the message is sent
     * @param message the message to be sent
     */
    public void sendMessage(UUID clientID, ReliabilityMessage message) {
        ClientHandler clientHandler = connectedClients.get(clientID);
        if (clientHandler != null) {
            clientHandler.sendMessage(encodeMessage(new DataMessage(LocalDateTime.now(), clientID, MessageType.DATA, message)));
        }
    }

    /**
     * Send a message to all connected clients simultaneously, simulating a broadcast
     *
     * @param message the message to be sent
     */
    public void sendMessageBroadcast(ReliabilityMessage message) {
        for (ClientHandler clientHandler : connectedClients.values()) {
            clientHandler.sendMessage(encodeMessage(new DataMessage(LocalDateTime.now(), clientHandler.getClientID(), MessageType.DATA, message)));
        }
    }

    /**
     * Return the next message in the buffer, blocking if the buffer is empty
     *
     * @return the next message in the buffer
     */
    public BasicMessage getMessage(){
        try {
            return upBuffer.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void disconnectClient(UUID clientID){
        //TODO: implement
    }

    public boolean isConnected() {
        return isConnected;
    }

    public Map<UUID, ClientHandler> getConnectedClients() {
        return connectedClients;
    }

    public BlockingQueue<BasicMessage> getUpBuffer() {
        return upBuffer;
    }
}
