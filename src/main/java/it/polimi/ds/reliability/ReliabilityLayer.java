package it.polimi.ds.reliability;

import it.polimi.ds.communication.CommunicationLayer;
import it.polimi.ds.communication.message.DataMessage;
import it.polimi.ds.utils.StablePriorityBlockingQueue;
import it.polimi.ds.vsync.KnowledgeableMessageType;
import it.polimi.ds.vsync.VSyncMessage;
import it.polimi.ds.vsync.faultTolerance.FaultRecovery;
import it.polimi.ds.vsync.view.ViewManager;
import it.polimi.ds.vsync.view.ViewManagerBuilder;
import it.polimi.ds.vsync.view.message.ViewManagerMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ReliabilityLayer {

    private final static Logger logger = LogManager.getLogger();

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
    private AcknowledgeMap ackMap = new AcknowledgeMap();

    private final Map<UUID, ReliabilityMessage> unstableReceivedMessages = new HashMap<>();

    private final Map<UUID, MessageTimer> unstableSentMessagesTimer = new HashMap<>();

    /**
     * Map of messages and their number of retries
     */
    private final HashMap<ReliabilityMessage, Integer> retries = new HashMap<>();

    /**
     * Buffer of messages to be sent to the upper VSync layer
     */
    private final StablePriorityBlockingQueue<ReliabilityMessage> upBuffer = new StablePriorityBlockingQueue<>();

    /**
     * Buffer of messages to be sent to the lower Communication layer
     */
    private final BlockingQueue<ReliabilityMessage> downBuffer = new LinkedBlockingQueue<>();

    private int eventID = 0;

    private final ViewManager viewManager;

    private boolean messageEnabled = false;

    private final FaultRecovery faultRecovery;

    public ReliabilityLayer(ViewManagerBuilder managerBuilder, FaultRecovery faultRecovery) {
        this.faultRecovery = faultRecovery;
        managerBuilder.setReliabilityLayer(this);
        this.handler = CommunicationLayer.defaultConfiguration(managerBuilder);
        viewManager = managerBuilder.create();
        new Thread(this::readMessage, "ReliabilityLayer::readMessage").start();
        new Thread(this::sendMessageBroadcast, "ReliabilityLayer::sendMessageBroadcast").start();
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
            if (messageReceived.messageType == MessageType.ACK)
                logger.trace("Received " + messageReceived.messageType + " message with ID " +
                        messageReceived.messageID + " for message " + messageReceived.referenceMessageID + " from " + senderUID);
            else
                logger.trace("Received " + messageReceived.messageType + " message with ID " +
                        messageReceived.messageID + " from " + senderUID);

            //Checks which scalar clock is higher and updates the eventID
            eventID = Math.max(messageReceived.timestamp.eventID(), eventID) + 1;
            ScalarClock timestamp = new ScalarClock(viewManager.getProcessID(),
                    ++eventID);

            if (messageReceived.messageType == MessageType.ACK) {
                UUID referencedMessageId = messageReceived.referenceMessageID;
                ackMap.receiveAck(referencedMessageId, senderUID, viewManager.getConnectedClients());

                //if all clients have acknowledged the message, remove it from the ackMap
                checkStable(referencedMessageId);
            } else {
                List<UUID> uuids;
                if (messageReceived.messageType == MessageType.DATA) {
                    uuids = new ArrayList<>(viewManager.getConnectedClients());
                    uuids.remove(senderUID);
                } else {
                    uuids = Collections.emptyList();
                }
                ackMap.receiveMessage(messageReceived.messageID, senderUID, uuids);
                sendAck(messageReceived, timestamp, senderUID);
                if (messageReceived.payload.knowledgeableMessageType == KnowledgeableMessageType.VIEW) {
                    viewManager.getBuffer().add(messageReceived);
                } else {
                    upBuffer.add(messageReceived);
                }
                unstableReceivedMessages.put(messageReceived.messageID, messageReceived);
                checkStable(messageReceived.messageID);
            }
        }
    }

    private void checkStable(UUID referencedMessageID) {
        if (ackMap.isComplete(referencedMessageID)) {
            boolean toLog = false;
            ReliabilityMessage message = unstableReceivedMessages.remove(referencedMessageID);
            if (message != null) {
                if (message.payload.knowledgeableMessageType == KnowledgeableMessageType.VIEW)
                    viewManager.getBuffer().markStable(message);
                else {
                    upBuffer.markStable(message);
                    toLog = true;
                }
                ackMap.remove(referencedMessageID);
            } else {
                MessageTimer messageTimer = unstableSentMessagesTimer.remove(referencedMessageID);
                if (messageTimer != null) {
                    ackMap.remove(referencedMessageID);
                    messageTimer.timer.cancel();
                    message = messageTimer.message;
                    if (messageTimer.message.payload.knowledgeableMessageType == KnowledgeableMessageType.VSYNC) {
                        toLog = true;
                    }
                }
            }
            if (toLog) {
                faultRecovery.logMessage((VSyncMessage) message.getPayload(), message.timestamp);
                logger.info("Logged message: " + message.messageID + " " + message.timestamp);
            }
        }
    }

    private void sendAck(ReliabilityMessage messageReceived, ScalarClock timestamp, UUID senderUID) {
        UUID ackMessageUID = UUID.randomUUID();
        ReliabilityMessage ackMessage = new ReliabilityMessage(ackMessageUID, messageReceived.messageID,
                timestamp);
        if (messageReceived.messageType == MessageType.SINGLE) {
            handler.sendMessage(senderUID, ackMessage);
            logger.trace("Sent ACK for message " + messageReceived.messageID + " with id "
                    + ackMessageUID + " to " + senderUID);
        } else {
            handler.sendMessageBroadcast(ackMessage);
            logger.trace("Sent ACK for message" + messageReceived.messageID + " with id "
                    + ackMessageUID + " to all clients");
        }
    }

    /**
     * Gets a message from the upper VSync layer and sends it to every client connected, simulating a broadcast
     * Waits for the acks to be received and resends the message if necessary.
     * If the message is not acknowledged after MAX_RETRIES the client is considered disconnected
     * and the communication layer is notified.
     */
    private void sendMessageBroadcast() {
        while (true) {
            synchronized (this) {
                while (!messageEnabled) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            try {
                ReliabilityMessage message = downBuffer.take();
                synchronized (this) {
                    while (!messageEnabled) {
                        wait();
                    }
                }
                if (message.timestamp.processID() == 0) {
                    message = new ReliabilityMessage(message.messageID, message.payload,
                            new ScalarClock(viewManager.getProcessID(), ++eventID));
                }
                logger.debug(
                        "Sending " + message.messageType + " message with ID " + message.messageID + " " + message.timestamp + " to" +
                                " all clients");
                handler.sendMessageBroadcast(message);
                ackMap.sendMessage(message.messageID, viewManager.getConnectedClients());
                checkDelivery(message);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Gets a message from the upper VSync layer, wraps it in a ReliabilityMessage and adds it to the downBuffer
     *
     * @param message the message to be sent
     */
    public void sendMessage(VSyncMessage message) {
        ScalarClock timestamp = new ScalarClock(viewManager.getProcessID(), ++eventID);
        ReliabilityMessage messageToSend = new ReliabilityMessage(UUID.randomUUID(), message, timestamp);
        downBuffer.add(messageToSend);
    }

    /**
     * Checks if the message has been acknowledged by all clients, if not resends it
     *
     * @param messageToCheck the message to be checked
     */
    private void checkDelivery(ReliabilityMessage messageToCheck) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                List<UUID> list = ackMap.missingAcks(messageToCheck.messageID);
                if (list.isEmpty()) {
                    if (messageToCheck.getPayload().knowledgeableMessageType == KnowledgeableMessageType.VSYNC) {
                        faultRecovery.logMessage((VSyncMessage) messageToCheck.getPayload(), messageToCheck.timestamp);
                        logger.info("CD Log message: " + messageToCheck.messageID + " " + messageToCheck.timestamp);
                    }
                    ackMap.remove(messageToCheck.messageID);
                    timer.cancel();
                } else {
                    list.forEach(id -> {
                        if (!retries.containsKey(messageToCheck)) {
                            retries.put(messageToCheck, 1);
                            handler.sendMessage(id, messageToCheck);
                        } else if (retries.get(messageToCheck) < MAX_RETRIES) {
                            logger.debug("Timer expired, trying to send the message again");
                            retries.put(messageToCheck, retries.get(messageToCheck) + 1);
                            handler.sendMessage(id, messageToCheck);
                        } else {
                            logger.debug("Timer expired again, disconnecting client");
                            handler.disconnectClient(id);
                            timer.cancel();
                        }
                    });
                }
            }
        }, TIMEOUT_RESEND, TIMEOUT_RESEND);
        unstableSentMessagesTimer.put(messageToCheck.messageID, new MessageTimer(timer, messageToCheck));
    }

    public void sendViewMessage(List<UUID> destinations, ViewManagerMessage message) {
        ScalarClock timestamp = new ScalarClock(viewManager.getProcessID(), ++eventID);
        ReliabilityMessage messageToSend;
        if (destinations.size() > 1) {
            messageToSend = new ReliabilityMessage(UUID.randomUUID(), message, timestamp);
        } else {
            messageToSend = new ReliabilityMessage(UUID.randomUUID(), message, MessageType.SINGLE, timestamp);
        }
        ackMap.sendMessage(messageToSend.messageID, destinations);
        for (UUID destination : destinations) {
            logger.debug("Sent message " + message.messageType + " " + messageToSend.timestamp + " with ID " + messageToSend.messageID + " to " + destination);
            handler.sendMessage(destination, messageToSend);
        }
        checkDelivery(messageToSend);
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
            return upBuffer.retrieveStable();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopMessageSending() {
        messageEnabled = false;
    }

    public void startMessageSending() {
        messageEnabled = true;
        synchronized (this) {
            notifyAll();
        }
    }

    public void waitStabilization() {
        try {
            Thread.sleep(1000);
            ackMap.waitEmpty();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleDisconnection(UUID clientUID) {
        unstableReceivedMessages.forEach((uuid, message) -> {
            if(uuid.equals(clientUID))
                unstableReceivedMessages.remove(uuid);
        });
        unstableSentMessagesTimer.forEach((uuid, messageTimer) -> {
            if(uuid.equals(clientUID)) {
                messageTimer.timer.cancel();
                unstableSentMessagesTimer.remove(uuid);
            }
        });
        ackMap = new AcknowledgeMap();
    }

    private record MessageTimer(Timer timer, ReliabilityMessage message) {

    }
}
