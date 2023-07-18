package it.polimi.ds.comunication;

import java.net.Socket;
import java.util.UUID;

/**
 * This class represent another connected client and can send or receive message from this
 */
public class ClientHandler implements Runnable{
    private final UUID connectedDeviceId;
    private final Socket socket;

    private final CommunicationLayer messageHandler;

    public ClientHandler(UUID connectedDeviceId, Socket socket, CommunicationLayer messageHandler) {
        this.connectedDeviceId = connectedDeviceId;
        this.socket = socket;
        this.messageHandler = messageHandler;
    }

    @Override
    public void run() {
    }
}
