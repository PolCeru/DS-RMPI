package it.polimi.ds.communication;

import com.google.gson.Gson;
import it.polimi.ds.utils.MessageGsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
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

    private final int random = new Random().nextInt();

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
        init();
    }

    public static CommunicationLayer defaultConfiguration() {
        return new CommunicationLayer(DEFAULT_PORT);
    }

    public static CommunicationLayer customConfiguration(int port) {
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

    private static BasicMessage decodeMessage(byte[] payload, int length) {
        return gson.fromJson(new String(payload, 0, length, StandardCharsets.UTF_8), DiscoveryMessage.class);
    }

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

                Thread.sleep(BROADCAST_INTERVAL);
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

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
                if (message.random <= this.random) {
                    addClient(message.getSenderUID(), socket);
                } else
                    System.err.println(
                            "My random is " + this.random + " but i received a message from " + socket.getInetAddress()
                                    .getHostAddress() + " with random " + message.random);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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

    synchronized private void addClient(UUID senderUID, Socket socket) {
        ClientHandler clientHandler = new ClientHandler(senderUID,
                socket, this);
        connectedClients.put(senderUID, clientHandler);
        new Thread(clientHandler).start();
        if (!isConnected) isConnected = !isConnected;
        System.out.println("Successfully connected with device " + socket.getInetAddress().getHostAddress());
    }

}
