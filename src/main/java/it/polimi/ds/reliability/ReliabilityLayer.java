package it.polimi.ds.reliability;

import it.polimi.ds.communication.CommunicationLayer;
import it.polimi.ds.communication.message.DataMessage;
import it.polimi.ds.vsync.KnowledgeableMessageType;
import it.polimi.ds.vsync.VSyncMessage;
import it.polimi.ds.vsync.faultTolerance.FaultRecovery;
import it.polimi.ds.vsync.view.ViewManager;
import it.polimi.ds.vsync.view.ViewManagerBuilder;
import it.polimi.ds.vsync.view.message.ViewManagerMessage;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

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
    private final PriorityBlockingQueue<ReliabilityMessage> upBuffer = new PriorityBlockingQueue<>();

    /**
     * Buffer of messages to be sent to the lower Communication layer
     */
    private final BlockingQueue<ReliabilityMessage> downBuffer = new LinkedBlockingQueue<>();

    private int eventID = 0;

    private final ViewManager viewManager;

    private final FaultRecovery faultRecovery;

    public ReliabilityLayer(ViewManagerBuilder managerBuilder, FaultRecovery faultRecovery) {
        this.faultRecovery = faultRecovery;
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

            //Checks which scalar clock is higher and updates the eventID
            ScalarClock timestamp = new ScalarClock(viewManager.getProcessID(),
                    (Math.max(messageReceived.timestamp.eventID(), eventID)) + 1);

            if (messageReceived.messageType == MessageType.ACK) {
                UUID referencedMessageId = messageReceived.referenceMessageID;
                ackMap.receiveAck(referencedMessageId, senderUID, viewManager.getConnectedClients());

                //if all clients have acknowledged the message, remove it from the ackMap
                if (ackMap.isComplete(referencedMessageId)) {
                    upBuffer.add(internalBuffer.remove(referencedMessageId));
                }
            } else if (messageReceived.messageType == MessageType.DATA) {
                //timestamp = new ScalarClock(viewManager.getProcessID(), eventID++);
                ackMap.receiveMessage(messageReceived.messageID, viewManager.getConnectedClients());
                UUID ackMessageUID = UUID.randomUUID();
                ReliabilityMessage ackMessage = new ReliabilityMessage(ackMessageUID, messageReceived.messageID,
                        timestamp);
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
        try {
            ReliabilityMessage message = downBuffer.take();
            internalBuffer.put(message.messageID, message);
            handler.sendMessageBroadcast(message);
            ackMap.sendMessage(message.messageID, viewManager.getConnectedClients());
            checkDelivery(message);
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
        ScalarClock timestamp = new ScalarClock(viewManager.getProcessID(), eventID++);
        ReliabilityMessage messageToSend = new ReliabilityMessage(UUID.randomUUID(), message, timestamp);
        downBuffer.add(messageToSend);
        sendMessageBroadcast();
    }

    public void sendViewMessage(List<UUID> destinations, ViewManagerMessage message) {
        ScalarClock timestamp = new ScalarClock(viewManager.getProcessID(), eventID++);
        ReliabilityMessage messageToSend = new ReliabilityMessage(UUID.randomUUID(), message, timestamp);
        internalBuffer.put(messageToSend.messageID, messageToSend);
        HashMap<UUID, Boolean> map = new HashMap<>();
        destinations.forEach(uuid -> map.put(uuid, Boolean.FALSE));
        ackMap.sendMessage(messageToSend.messageID, destinations);
        for (UUID destination : destinations) {
            System.out.println("Sent message " + message.messageType + " to " + destination);
            handler.sendMessage(destination, messageToSend);
            checkDelivery(messageToSend);
        }
    }

    /**
     * Checks if the message has been acknowledged by all clients, if not resends it
     *
     * @param messageToSend the message to be checked
     */
    private void checkDelivery(ReliabilityMessage messageToSend) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                List<UUID> list = ackMap.missingAcks(messageToSend.messageID);
                if (list.isEmpty()) {
                    //TODO: log the message in the FaultRecovery class
                    ackMap.remove(messageToSend.messageID);
                    timer.cancel();
                } else {
                    list.forEach(id -> {
                        if (!retries.containsKey(messageToSend)) {
                            retries.put(messageToSend, 1);
                            handler.sendMessage(id, messageToSend);
                        } else if (retries.get(messageToSend) <= MAX_RETRIES) {
                            retries.put(messageToSend, retries.get(messageToSend) + 1);
                            handler.sendMessage(id, messageToSend);
                        } else handler.disconnectClient(id);
                    });
                }
            }
        }, 100, TIMEOUT_RESEND);
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

    public ReliabilityMessage getMessage() {
        try {
            return upBuffer.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
