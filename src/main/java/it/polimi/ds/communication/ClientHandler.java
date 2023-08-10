package it.polimi.ds.communication;


import it.polimi.ds.communication.message.DataMessage;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * This class represent another connected client and can send or receive message from this
 */
public class ClientHandler implements Runnable {

    /**
     * The clientID is the unique identifier of this client
     */
    private final UUID clientID;

    /**
     * The socket is the connection between this client and the other client
     */
    private final Socket socket;

    /**
     * The messageHandler is the class that will handle the communication between this client and other clients
     */
    private final CommunicationLayer messageHandler;

    public ClientHandler(UUID clientID, Socket socket, CommunicationLayer messageHandler) {
        this.clientID = clientID;
        this.socket = socket;
        this.messageHandler = messageHandler;
    }

    /**
     * start a new thread to receive message from the client
     */
    @Override
    public void run() {
        new Thread(this::receiveMessage, "ClientHandler:" + clientID).start();
    }

    /**
     * send a message to the client writing bytes in the output stream
     *
     * @param payload the message in bytes to be sent
     * @throws RuntimeException if there is an error in the output stream
     */
    public void sendMessage(byte[] payload) {
        if (messageHandler.isConnected()) {
            try {
                socket.getOutputStream().write(payload);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * initialize the input stream and stay in a loop to receive a message and send it to the messageHandler
     *
     * @throws RuntimeException if there is an error in the input/output stream
     */
    private void receiveMessage() {
        while (messageHandler.isConnected()) {
            byte[] payload;
            try {
                payload = socket.getInputStream().readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            DataMessage message = CommunicationLayer.gson.fromJson(new String(payload, StandardCharsets.UTF_8), DataMessage.class);
            messageHandler.getUpBuffer().add(message);
        }
    }

    /**
     * @return the clientID of this client
     */
    public UUID getClientID() {
        return clientID;
    }
}
