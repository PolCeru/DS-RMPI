package it.polimi.ds.comunication;

import com.google.gson.JsonParseException;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
