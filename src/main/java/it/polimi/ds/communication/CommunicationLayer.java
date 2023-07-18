package it.polimi.ds.communication;

import com.google.gson.Gson;
import it.polimi.ds.utils.MessageGsonBuilder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

public class CommunicationLayer {
    private static final String BROADCAST_ADDR = "255.255.255.255";
    /**
     * Default communication port
     */
    private static final int DEFAULT_PORT = 4445;
    private static final int BROADCAST_INTERVAL = 10000;


    /**
     * Identify this machine uniquely
     */
    private final UUID uuid = UUID.randomUUID();

    /**
     * Show if this piece is still locking for a device networks
     */
    private volatile boolean isConnected = false;

    /**
     * Serializer of basic messages
     */
    private static final Gson gson = new MessageGsonBuilder().registerMessageAdapter()
            .registerLocalDateTimeAdapter()
            .create();

    private final Map<UUID, ClientHandler> connectedClients = new HashMap<>();

    private final int port;

    private CommunicationLayer(int port) {
        this.port = port;
    }

    public static CommunicationLayer defaultConfiguration(){
        return new CommunicationLayer(DEFAULT_PORT);
    }

    public static CommunicationLayer customConfiguration(int port){
        return new CommunicationLayer(port);
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
                DiscoveryMessage message = new DiscoveryMessage(LocalDateTime.now(), uuid, port);
                byte[] sendData = encodeMessage(message);

                DatagramPacket packet = new DatagramPacket(sendData, sendData.length,
                        InetAddress.getByName(BROADCAST_ADDR), port);

                socket.send(packet);
                System.out.println("Broadcast message sent: " + message);

                Thread.sleep(BROADCAST_INTERVAL);
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void startDiscoveryListener() {
        final int bufferSize = 1024;
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] receiveData = new byte[bufferSize];

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
            ClientHandler clientHandler = new ClientHandler(message.getSenderUID(), new Socket(address, message.getPort()), this);
            connectedClients.put(message.getSenderUID(), clientHandler);
            new Thread(clientHandler).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
