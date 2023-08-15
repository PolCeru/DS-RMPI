package it.polimi.ds.reliability;

import it.polimi.ds.communication.CommunicationLayer;
import it.polimi.ds.communication.message.DataMessage;
import it.polimi.ds.vsync.VSyncMessage;
import it.polimi.ds.vsync.view.ViewManager;
import it.polimi.ds.vsync.view.ViewManagerBuilder;
import it.polimi.ds.vsync.view.message.ViewManagerMessage;

import java.util.*;
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

    private final ViewManager viewManager;

    public ReliabilityLayer(ViewManagerBuilder managerBuilder) {
        managerBuilder.setReliabilityLayer(this);
        this.handler = CommunicationLayer.defaultConfiguration(managerBuilder);
        viewManager = managerBuilder.create();
        new Thread(this::readMessage).start();
    }

    private void readMessage() {
        while (handler.isConnected()) {
            DataMessage dataMessage = (DataMessage) handler.getMessage();
            UUID senderUID = dataMessage.getSenderUID();
            ReliabilityMessage messageReceived = dataMessage.getPayload();

            if (messageReceived.getMessageType() == MessageType.ACK) {
                //gets the referencedMessage, which is the message that matches message.getReferenceMessageID()
                //TODO: if exception is thrown it means that an ack has been received for a non-existing message |*|
                // --> client is "hacked" -> disconnect?
                // |*| since the message is not in the ackMap --> question: can I still receive an ack for a message
                // that has been removed from the ackMap because it has been acknowledged by all clients? answer:
                // yes, just discard the ack
                ReliabilityMessage referencedMessage = ackMap.entrySet().stream()
                        .filter(entry -> entry.getKey().getMessageID().equals(messageReceived.getReferenceMessageID()))
                        .findFirst()
                        .orElseThrow()
                        .getKey();
                //and sets the ack flag to true
                ackMap.get(referencedMessage).put(senderUID, true);

                //if all clients have acknowledged the message, remove it from the ackMap
                if (ackMap.get(messageReceived).values().stream().allMatch(b -> b)) {
                    ackMap.remove(messageReceived);
                    upBuffer.add(messageReceived);
                }
            }
            //TODO: handle logic if an ack arrives while ackMap is already true -> discard the ack
            // if(ackMap.get(referencedMessage).get(senderUID))
            else if (messageReceived.getMessageType() == MessageType.DATA) {
                ReliabilityMessage ackMessage = new ReliabilityMessage(UUID.randomUUID(), messageReceived.getMessageID());
                handler.sendMessage(senderUID, ackMessage);
            }
        }
    }

    /**
     * Gets a message from the upper VSynch layer and sends it to every client connected, simulating a broadcast
     * Waits for the acks to be received and resends the message if necessary.
     * If the message is not acknowledged after MAX_RETRIES the client is considered disconnected
     * and the communication layer is notified.
     */
    private void sendMessageBroadcast() {
        Timer timer = new Timer();
        try {
            ReliabilityMessage message = downBuffer.take();
            HashMap<UUID, Boolean> singleAck = new HashMap<>();
            int TIMEOUT_RESEND = 10000;

            handler.sendMessageBroadcast(message);
            //Init ack map for this message
            //FIXME: add a check to avoid keeping track of the sender
            // DONE; MUST CHECK CORRECTNESS
            for (UUID uuid : handler.getConnectedClients().keySet()) {
                if (!uuid.equals(viewManager.getClientUID()))
                    singleAck.put(uuid, false);
            }
            ackMap.put(message, singleAck);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    ackMap.get(message).forEach((UUID, ackFlag) -> {
                        //If the message is not acknowledged
                        //and is not in the retries map, add it and send it again
                        //otherwise check if the number of retries is less than MAX_RETRIES
                        //and send it again or disconnect the client
                        if (!ackFlag)
                            if (!retries.containsKey(message)) {
                                retries.put(message, 1);
                                handler.sendMessage(UUID, message);
                            } else if (retries.get(message) <= MAX_RETRIES) {
                                retries.put(message, retries.get(message) + 1);
                                handler.sendMessage(UUID, message);
                            } else handler.disconnectClient(UUID);
                    });
                }
            }, 0, TIMEOUT_RESEND);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a message from the upper VSynch layer, wraps it in a ReliabilityMessage and adds it to the downBuffer
     *
     * @param message the message to be sent
     */
    public void sendMessage(VSyncMessage message) {
        ReliabilityMessage messageToSend = new ReliabilityMessage(UUID.randomUUID(), message);
        downBuffer.add(messageToSend);
        sendMessageBroadcast();
    }

    public void sendViewMessage(List<UUID> destinations, ViewManagerMessage message) {
        Timer timer = new Timer();
        ReliabilityMessage messageToSend = new ReliabilityMessage(UUID.randomUUID(), message);
        HashMap<UUID, Boolean> map = new HashMap<>();
        destinations.forEach(uuid -> map.put(uuid, Boolean.FALSE));
        for (UUID destination : destinations) {
            ackMap.put(messageToSend, map);
            handler.sendMessage(destination, messageToSend);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    ackMap.get(messageToSend).forEach((UUID, ackFlag) -> {
                        if (!ackFlag)
                            if (!retries.containsKey(messageToSend)) {
                                retries.put(messageToSend, 1);
                                handler.sendMessage(UUID, messageToSend);
                            } else if (retries.get(messageToSend) <= MAX_RETRIES) {
                                retries.put(messageToSend, retries.get(messageToSend) + 1);
                                handler.sendMessage(UUID, messageToSend);
                            } else handler.disconnectClient(UUID);
                    });
                }
            }, 0, 5000);
        }
    }

    /**
     * Used only for testing purposes
     */
    void setCommunicationLayer(CommunicationLayer handler) {
        this.handler = handler;
    }

    /**
     * Used only for testing purposes
     */
    ViewManager getViewManager() {
        return viewManager;
    }
}
