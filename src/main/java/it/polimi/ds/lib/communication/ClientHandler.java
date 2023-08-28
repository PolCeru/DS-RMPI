package it.polimi.ds.lib.communication;

import it.polimi.ds.lib.communication.message.DataMessage;

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
     * The clientUID is the unique identifier of this client
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

    private final Socket socket;

    public ClientHandler(UUID clientUID, Socket socket, CommunicationLayer messageHandler) throws IOException {
        this.clientUID = clientUID;
        this.socket = socket;
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
                System.err.println("couldn't send" + e.getMessage());
                close();
                messageHandler.disconnectClient(clientUID);
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
            } catch (IOException e) {
                System.err.println("couldn't read " + e.getMessage());
                close();
                messageHandler.disconnectClient(clientUID);
                break;
            }
        }
    }

    public void close() {
        try {
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("couldn't close " + e.getMessage());
        }
    }

    /**
     * @return the clientUID of this client
     */
    public UUID getClientUID() {
        return clientUID;
    }
}
