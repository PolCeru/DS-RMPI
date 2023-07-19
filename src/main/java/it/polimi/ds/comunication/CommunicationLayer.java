package it.polimi.ds.comunication;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.polimi.ds.reliability.ReliabilityMessage;
import it.polimi.ds.utils.RuntimeTypeAdapterFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CommunicationLayer {
    private static final String BROADCAST_ADDR = "255.255.255.255";
    /**
     * Default communication port
     */
    private static final int PORT = 4445;
    /**
     * Interval between two broadcast messages when discovering other devices
     */
    private static final int BROADCAST_INTERVAL = 10000;
    /**
     * Show if this piece is still locking for a device networks
     */
    private volatile boolean isConnected = false;
    /**
     * Identify this machine uniquely
     */
    private final UUID uuid = UUID.randomUUID();

    /**
     * Serializer of basic messages
     */
    static final Gson gson = new GsonBuilder().registerTypeAdapterFactory(RuntimeTypeAdapterFactory.of(
                    BasicMessage.class, "type")
            .registerSubtype(DataMessage.class, "data")
            .registerSubtype(DiscoveryMessage.class, "discovery")).create();

    private final Map<UUID, ClientHandler> connectedClients = new HashMap<>();
    private final BlockingQueue<BasicMessage> upBuffer = new LinkedBlockingQueue<>();

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
     * Serialize and convert bytes into a BasicMessage
     *
     * @param payload the payload to convert in message
     * @return the message generated from the payload
     */
    private static BasicMessage decodeMessage(byte[] payload) {
        return gson.fromJson(new String(payload, StandardCharsets.UTF_8), BasicMessage.class);
    }

    private void init() {
        new Thread(this::startDiscoveryListener).start();
        Thread discoverySender = new Thread(this::startDiscoverySender);
        discoverySender.start();
        try {
            discoverySender.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void startDiscoverySender() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);

            while (!isConnected) {
                DiscoveryMessage message = new DiscoveryMessage(LocalDateTime.now(), uuid);
                byte[] sendData = encodeMessage(message);

                DatagramPacket packet = new DatagramPacket(sendData, sendData.length,
                        InetAddress.getByName(BROADCAST_ADDR), PORT);

                socket.send(packet);
                System.out.println("Broadcast message sent: " + message);

                Thread.sleep(BROADCAST_INTERVAL);
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void startDiscoveryListener() {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            byte[] receiveData = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(packet);

                DiscoveryMessage message = (DiscoveryMessage) decodeMessage(packet.getData());
                InetAddress address = packet.getAddress();
                addClient(message, address);
                isConnected = true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addClient(DiscoveryMessage message, InetAddress address) {
        try {
            ClientHandler clientHandler = new ClientHandler(message.getSenderUID(), new Socket(address, PORT), this);
            connectedClients.put(message.getSenderUID(), clientHandler);
            new Thread(clientHandler).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
