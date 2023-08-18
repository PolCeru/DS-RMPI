package it.polimi.ds.communication;

import it.polimi.ds.communication.message.DataMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
    private final UUID clientUID;

    /**
     * inputStream is the stream used to receive message from the client
     */
    private final DataInputStream inputStream;

    /**
     * outputStream is the stream used to send message to the client
     */
    private final DataOutputStream outputStream;

    /**
     * The messageHandler is the class that will handle the communication between this client and other clients
     */
    private final CommunicationLayer messageHandler;

    public ClientHandler(UUID clientID, Socket socket, CommunicationLayer messageHandler) throws IOException {
        this.clientUID = clientID;
        this.inputStream = new DataInputStream(socket.getInputStream());
        this.outputStream = new DataOutputStream(socket.getOutputStream());
        this.messageHandler = messageHandler;
    }

    /**
     * start a new thread to receive message from the client
     */
    @Override
    public void run() {
        new Thread(this::receiveMessage, "ClientHandler:" + clientUID).start();
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
                outputStream.writeInt(payload.length);
                outputStream.write(payload);
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
                int length = inputStream.readInt();
                payload = new byte[length];
                inputStream.read(payload);
                DataMessage message = CommunicationLayer.gson.fromJson(new String(payload, StandardCharsets.UTF_8), DataMessage.class);
                messageHandler.getUpBuffer().add(message);
                System.out.println("Message added to the Communication Layer upBuffer");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @return the clientID of this client
     */
    public UUID getClientUID() {
        return clientUID;
    }
}
