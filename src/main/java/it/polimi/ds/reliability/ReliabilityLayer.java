package it.polimi.ds.reliability;

import it.polimi.ds.communication.CommunicationLayer;
import it.polimi.ds.communication.message.DataMessage;
import it.polimi.ds.vsync.KnowledgeableMessageType;
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
     * Timeout before resending a message
     */
    int TIMEOUT_RESEND = 10000;

    /**
     * The communication layer to use to send and receive messages
     */
    private CommunicationLayer handler;

    /**
     * Map of messages to be acknowledged, for each message a map of clients and their ack status
     */
    private final AcknowledgeMap ackMap = new AcknowledgeMap();

    private final Map<UUID, ReliabilityMessage> internalBuffer = new HashMap<>();

    /**
     * Map of messages and their number of retries
     */
    private final HashMap<ReliabilityMessage, Integer> retries = new HashMap<>();

    /**
     * Buffer of messages to be sent to the upper VSync layer
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
        try {
            this.handler.getLatch().await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        while (handler.isConnected()) {
            DataMessage dataMessage = (DataMessage) handler.getMessage();
            UUID senderUID = dataMessage.senderUID;
            ReliabilityMessage messageReceived = dataMessage.payload;
            System.out.println("Received " + messageReceived.messageType + " message with ID " +
                    messageReceived.messageID + " from " + senderUID);

            if (messageReceived.messageType == MessageType.ACK) {
                UUID referencedMessageId = messageReceived.referenceMessageID;
                ackMap.receiveAck(referencedMessageId, senderUID, viewManager.getConnectedClients());

                //if all clients have acknowledged the message, remove it from the ackMap
                if (ackMap.isComplete(referencedMessageId)) {
                    ackMap.remove(referencedMessageId);
                    upBuffer.add(internalBuffer.remove(referencedMessageId));
                }
            }
            //TODO: handle logic if an ack arrives while ackMap is already true -> discard the ack
            // if(ackMap.get(referencedMessage).get(senderUID))
            else if (messageReceived.messageType == MessageType.DATA) {
                ackMap.receiveMessage(messageReceived.messageID, viewManager.getConnectedClients());
                UUID ackMessageUID = UUID.randomUUID();
                ReliabilityMessage ackMessage = new ReliabilityMessage(ackMessageUID, messageReceived.messageID);
                handler.sendMessageBroadcast(ackMessage);
                System.out.println("Sent ACK for message " + messageReceived.messageID + " with id "
                        + ackMessageUID + " to " + senderUID);

                if (messageReceived.payload.knowledgeableMessageType == KnowledgeableMessageType.VIEW) {
                    System.out.println("View message received");
                    viewManager.handleViewMessage((ViewManagerMessage) messageReceived.payload);
                }
            }
        }
    }

    /**
     * Gets a message from the upper VSync layer and sends it to every client connected, simulating a broadcast
     * Waits for the acks to be received and resends the message if necessary.
     * If the message is not acknowledged after MAX_RETRIES the client is considered disconnected
     * and the communication layer is notified.
     */
    private void sendMessageBroadcast() {
        Timer timer = new Timer();
        try {
            ReliabilityMessage message = downBuffer.take();
            internalBuffer.put(message.messageID, message);
            handler.sendMessageBroadcast(message);
            ackMap.sendMessage(message.messageID, viewManager.getConnectedClients());
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    ackMap.missingAcks(message.messageID).forEach((id) -> {
                        //If the message is not acknowledged
                        //and is not in the retries map, add it and send it again
                        //otherwise check if the number of retries is less than MAX_RETRIES
                        //and send it again or disconnect the client
                        if (!retries.containsKey(message)) {
                            retries.put(message, 1);
                            handler.sendMessage(id, message);
                        } else if (retries.get(message) <= MAX_RETRIES) {
                            retries.put(message, retries.get(message) + 1);
                            handler.sendMessage(id, message);
                        } else handler.disconnectClient(id);
                        //TODO disconnect: handle here the case
                    });
                }
            }, 500, TIMEOUT_RESEND);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a message from the upper VSync layer, wraps it in a ReliabilityMessage and adds it to the downBuffer
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
        internalBuffer.put(messageToSend.messageID, messageToSend);
        HashMap<UUID, Boolean> map = new HashMap<>();
        destinations.forEach(uuid -> map.put(uuid, Boolean.FALSE));
        ackMap.sendMessage(messageToSend.messageID, destinations);
        for (UUID destination : destinations) {
            System.out.println("Sent message " + message.messageType + " to " + destination);
            handler.sendMessage(destination, messageToSend);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    ackMap.missingAcks(messageToSend.messageID).forEach(id -> {
                        if (!retries.containsKey(messageToSend)) {
                            retries.put(messageToSend, 1);
                            handler.sendMessage(id, messageToSend);
                        } else if (retries.get(messageToSend) <= MAX_RETRIES) {
                            retries.put(messageToSend, retries.get(messageToSend) + 1);
                            handler.sendMessage(id, messageToSend);
                        } else handler.disconnectClient(id);
                    });
                }
            }, 500, TIMEOUT_RESEND);
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
