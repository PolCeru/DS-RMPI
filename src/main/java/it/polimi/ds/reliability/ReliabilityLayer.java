package it.polimi.ds.reliability;

import it.polimi.ds.communication.CommunicationLayer;
import it.polimi.ds.communication.message.DataMessage;
import it.polimi.ds.communication.message.MessageType;
import it.polimi.ds.vsync.VSyncMessage;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ReliabilityLayer {

    /**
     * Maximum number of retries before considering a client disconnected
     */
    private final int MAX_RETRIES = 2;

    /**
     * The communication layer to use to send and receive messages
     */
    private final CommunicationLayer handler;

    /**
     * Map of messages to be acknowledged, for each message a map of clients and their ack status
     */
    private final HashMap<ReliabilityMessage, HashMap<UUID, Boolean>> ackMap = new HashMap<>();

    /**
     * Map of messages and their number of retries
     */
    private final HashMap<ReliabilityMessage, Integer> retries = new HashMap<>();

    /**
     * Buffer of messages to be sent to the upper VSynch layer
     */
    private final BlockingQueue<ReliabilityMessage> upBuffer = new LinkedBlockingQueue<>();

    /**
     * Buffer of messages to be sent to the lower Communication layer
     */
    private final BlockingQueue<ReliabilityMessage> downBuffer = new LinkedBlockingQueue<>();

    public ReliabilityLayer(CommunicationLayer handler) {
        this.handler = handler;
        new Thread(this::readMessage).start();
    }

    /**
     * Stays in a loop waiting for messages to be received from the lower Communication layer and
     */
    private void readMessage() {
        while (handler.isConnected()) {
            DataMessage dataMessage = (DataMessage) handler.getMessage();
            ReliabilityMessage message = dataMessage.getPayload();
            UUID senderUID = dataMessage.getSenderUID();

            if (ackMap.containsKey(message)) {
                HashMap<UUID, Boolean> singleAck = new HashMap<>();
                for (UUID uuid : handler.getConnectedClients().keySet())
                    singleAck.put(uuid, false);
                ackMap.put(message, singleAck);
            } else if (message.getMessageType() == MessageType.ACK && !ackMap.get(message).get(senderUID)) {
                ackMap.get(message).put(senderUID, true);
            }

            if (ackMap.get(message).values().stream().allMatch(b -> b)) {
                ackMap.remove(message);
                upBuffer.add(message);
            }
        }
    }

    /**
     * Gets a message from the upper VSynch layer and sends it to every client connected, simulating a broadcast
     * Waits for the acks to be received and resends the message if necessary.
     * If the message is not acknowledged after MAX_RETRIES the client is considered disconnected
     * and the communication layer is notified.
     *
     */
    private void sendMessageBroadcast() {
        Timer timer = new Timer();
        try {
            ReliabilityMessage message = downBuffer.take();
            handler.sendMessageBroadcast(message);
            /**
             * Timeout in milliseconds for the acks to be received
             */
            int TIMEOUT = 10000;
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    ackMap.get(message).forEach((UUID, ackFlag) -> {
                        if (!ackFlag)
                            if(!retries.containsKey(message)){
                                retries.put(message, 1);
                                handler.sendMessage(UUID, message);
                            }
                            else if (retries.get(message) <= MAX_RETRIES){
                                retries.put(message, retries.get(message) + 1);
                                handler.sendMessage(UUID, message);
                            }
                            else handler.disconnectClient(UUID);
                    });
                }
            }, 0, TIMEOUT);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a message from the upper VSynch layer, wraps it in a ReliabilityMessage and adds it to the downBuffer
     * @param message the message to be sent
     */
    private void sendMessage(VSyncMessage message) {
        //called by VSynchLayer, converts to reliabilityMessage adds the message to the downBuffer and calls sendMessageBroadcast
        //is downBuffer useful at all at this point? can we just send the message directly?
        ReliabilityMessage messageToSend = new ReliabilityMessage(UUID.randomUUID(), MessageType.DATA, message);
        downBuffer.add(messageToSend);
        sendMessageBroadcast();
    }
}
