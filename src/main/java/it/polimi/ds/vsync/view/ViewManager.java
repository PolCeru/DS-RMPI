package it.polimi.ds.vsync.view;

import it.polimi.ds.communication.CommunicationLayer;
import it.polimi.ds.communication.message.DiscoveryMessage;
import it.polimi.ds.reliability.AcknowledgeMap;
import it.polimi.ds.reliability.ReliabilityLayer;
import it.polimi.ds.reliability.ReliabilityMessage;
import it.polimi.ds.utils.StablePriorityBlockingQueue;
import it.polimi.ds.vsync.VSyncLayer;
import it.polimi.ds.vsync.faultTolerance.Checkpoint;
import it.polimi.ds.vsync.faultTolerance.FaultRecovery;
import it.polimi.ds.vsync.view.message.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ViewManager {

    /**
     * Random number used by the protocol to decide which device is the master and should start the connection
     */
    private final int random = new Random().nextInt();

    /**
     * Identify this machine uniquely
     */
    private final UUID clientUID = UUID.randomUUID();

    private final CommunicationLayer communicationLayer;

    private final ReliabilityLayer reliabilityLayer;

    private final FaultRecovery faultRecovery;

    private boolean isConnected = false;

    private int processID;

    private int clientsProcessIDCounter;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<UUID> realViewManager = Optional.empty();

    private final List<UUID> connectedHosts = new LinkedList<>();

    private final List<UUID> waitingHosts = new LinkedList<>();

    private final StablePriorityBlockingQueue<ReliabilityMessage> buffer = new StablePriorityBlockingQueue<>();

    private final BlockingQueue<ConfirmViewChangeMessage> confirmBuffer = new LinkedBlockingQueue<>();

    private ViewChangeList viewChangeList;
    private static final Logger logger = LogManager.getLogger();

    public ViewManager(VSyncLayer vSyncLayer, ReliabilityLayer reliabilityLayer,
                       CommunicationLayer communicationLayer, FaultRecovery faultRecovery) {
        this.faultRecovery = faultRecovery;
        this.communicationLayer = communicationLayer;
        this.reliabilityLayer = reliabilityLayer;
        new Thread("ViewManager") {
            @Override
            public void run() {
                while (true) {
                    try {
                        ViewManagerMessage message = getMessage();
                        handleViewMessage(message);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    public void handleViewMessage(ViewManagerMessage baseMessage) {
        if (baseMessage.messageType != ViewChangeType.CONFIRM)
            logger.debug("Received stable message " + baseMessage.messageType);
        switch (baseMessage.messageType) {
            case CONFIRM -> {
                ConfirmViewChangeMessage message = (ConfirmViewChangeMessage) baseMessage;
                logger.debug("Received confirm " + message.confirmedAction + " from " + message.senderUid);
                if (connectedHosts.isEmpty()) {
                    waitingHosts.remove(message.senderUid);
                    assert waitingHosts.isEmpty();
                    connectedHosts.add(message.senderUid);
                    isConnected = true;
                    reliabilityLayer.startMessageSending();
                } else {
                    switch (message.confirmedAction) {
                        case FREEZE_VIEW -> //received by manager when a group member has frozen its view
                                confirmBuffer.add(message);
                        case NEW_HOST -> //received by manager when a group member connected with new host
                                confirmBuffer.add(message);
                        case CHECKPOINT -> //received by manager when a group member received the checkpoint
                                confirmBuffer.add(message);
                        case INIT_VIEW -> {
                            waitingHosts.remove(message.senderUid);
                            assert waitingHosts.isEmpty();
                            connectedHosts.add(message.senderUid);
                            confirmBuffer.add(message);
                        }
                        case CONNECT_REQ -> {
                            //received by group member from the new host when it's connected
                            waitingHosts.remove(message.senderUid);
                            assert waitingHosts.isEmpty();
                            connectedHosts.add(message.senderUid);
                            reliabilityLayer.sendViewMessage(Collections.singletonList(realViewManager.get()),
                                    new ConfirmViewChangeMessage(clientUID, ViewChangeType.NEW_HOST));
                            viewChangeList = null;
                        }
                        case RESTART_VIEW -> {
                            confirmBuffer.add(message);
                        }
                    }
                }
            }
            case INIT_VIEW -> {
                InitialTopologyMessage message = (InitialTopologyMessage) baseMessage;
                if (connectedHosts.isEmpty()) {
                    realViewManager = Optional.of(message.viewManagerId);
                    if (message.topology.size() == 1) {
                        //only the viewManager connected
                        handleNewConnection(message.viewManagerId);
                        isConnected = true;
                        reliabilityLayer.startMessageSending();
                        processID = message.destinationProcessID;
                    } else {
                        //TODO implement method that create a waitingList with provided list,
                        // confirm that's ready to viewManager and wait for all connections
                        if (viewChangeList == null) viewChangeList = ViewChangeList.fromExpectedUsers(message.topology);
                        else viewChangeList.setExpectedUsers(message.topology);
                        //add the view manager to the connected list
                        handleNewConnection(message.viewManagerId);
                        processID = message.destinationProcessID;
                        viewChangeList.addConnectedUser(message.viewManagerId);
                        //add all the others clients
                        while (!viewChangeList.isComplete()) {
                            try {
                                ConnectRequestMessage connectMessage = (ConnectRequestMessage) buffer.retrieveStable().payload;
                                logger.debug("Received message " + connectMessage.messageType);
                                handleNewConnection(connectMessage.senderUid);
                                viewChangeList.addConnectedUser(connectMessage.senderUid);
                                reliabilityLayer.sendViewMessage(Collections.singletonList(connectMessage.senderUid),
                                        new ConfirmViewChangeMessage(clientUID, ViewChangeType.CONNECT_REQ));
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        viewChangeList = null;
                    }
                } else {
                    logger.fatal("Received INIT_VIEW message while already connected");
                }
                reliabilityLayer.sendViewMessage(List.of(realViewManager.get()), new ConfirmViewChangeMessage(clientUID,
                        ViewChangeType.INIT_VIEW));
            }
            case FREEZE_VIEW -> {
                reliabilityLayer.stopMessageSending();
                reliabilityLayer.waitStabilization();
                logger.debug("Freeze view complete");
                reliabilityLayer.sendViewMessage(List.of(realViewManager.get()), new ConfirmViewChangeMessage(clientUID,
                        ViewChangeType.FREEZE_VIEW));
            }
            case NEW_HOST -> {
                //received by group member from manager when new host try to connect
                NewHostMessage message = (NewHostMessage) baseMessage;
                if (viewChangeList == null)
                    viewChangeList = ViewChangeList.fromExpectedUsers(Collections.singletonList(message.newHostId));
                else logger.error("Received NEW_HOST message while view change list not null");
                handleNewHost(message.newHostId, message.newHostRandom, message.newHostAddress);
            }
            case CONNECT_REQ -> {
                //received by new host from non view manager if group already present
                ConnectRequestMessage message = (ConnectRequestMessage) baseMessage;
                if (viewChangeList == null) {
                    viewChangeList = ViewChangeList.fromUnexpectedUser(message.senderUid);
                    logger.debug("Received CONNECT_REQ message before receiving NEW_HOST message");
                }
                handleNewConnection(message.senderUid);
                reliabilityLayer.sendViewMessage(Collections.singletonList(message.senderUid),
                        new ConfirmViewChangeMessage(clientUID, ViewChangeType.CONNECT_REQ));
            }
            case RESTART_VIEW -> {
                reliabilityLayer.sendViewMessage(Collections.singletonList(realViewManager.get()), new ConfirmViewChangeMessage(clientUID,
                        ViewChangeType.RESTART_VIEW));
                endViewFreeze();
            }
            case CHECKPOINT -> {
                CheckpointMessage message = (CheckpointMessage) baseMessage;
                ArrayList<Checkpoint> checkpointList = new ArrayList<>();
                checkpointList.add(message.checkpoint);
                faultRecovery.addCheckpoints(checkpointList);
                reliabilityLayer.sendViewMessage(Collections.singletonList(realViewManager.get()), new ConfirmViewChangeMessage(clientUID,
                        ViewChangeType.CHECKPOINT));
            }
            //TODO: case RECOVER_REQUEST e RECOVERY_PACKET, (remember to start a checkpoint when a user
            // disconnects/connects)
            default -> throw new RuntimeException("Unexpected message type " + baseMessage.messageType);
        }
    }

    /**
     * Handle what happen when we receive a {@link DiscoveryMessage} over the TCP connection
     *
     * @param newHostId the new host UUID
     */
    public synchronized void handleNewConnection(UUID newHostId) {
        //already connected, so discard
        if (connectedHosts.contains(newHostId)) return;
        // first connection between device
        if (connectedHosts.isEmpty()) {
            communicationLayer.stopDiscoverySender();
        }
        connectedHosts.add(newHostId);
    }

    private List<UUID> getCompleteTopology() {
        List<UUID> uuids = new ArrayList<>(connectedHosts);
        uuids.add(clientUID);
        return uuids;
    }

    /**
     * Handle what happen when a new host is detected over the broadcast network
     *
     * @param newHostId      the new host UUID
     * @param newHostRandom  the new host random number
     * @param newHostAddress the new host IP address
     */
    public synchronized void handleNewHost(UUID newHostId, int newHostRandom, InetAddress newHostAddress) {
        // already connected, so discard
        if (connectedHosts.contains(newHostId) || waitingHosts.contains(newHostId) || newHostId.equals(clientUID))
            return;
        // first connection between devices
        if (connectedHosts.isEmpty()) {
            if (random < newHostRandom) {
                if (realViewManager.isEmpty()) {
                    processID = 1;
                    clientsProcessIDCounter = 1;
                }
                communicationLayer.stopDiscoverySender();
                startConnection(newHostId, newHostAddress);
                reliabilityLayer.sendViewMessage(Collections.singletonList(newHostId),
                        new InitialTopologyMessage(clientUID, ++clientsProcessIDCounter, getCompleteTopology()));
            } else {
                logger.info("New host:" + newHostAddress.getHostAddress() + "(random " + newHostRandom + ")");
            }
        } else if (realViewManager.isEmpty()) { //what to do when you are the real manager
            //TODO: handle creation logic and propagation of the view
            waitingHosts.add(newHostId);
            startFreezeView();
            communicationLayer.initConnection(newHostAddress, newHostId);
            AcknowledgeMap acknowledgeMap = new AcknowledgeMap();
            //sent init view message to new host
            InitialTopologyMessage initialTopologyMessage = new InitialTopologyMessage(clientUID, ++clientsProcessIDCounter, getCompleteTopology());
            reliabilityLayer.sendViewMessage(Collections.singletonList(newHostId),
                    initialTopologyMessage);
            acknowledgeMap.sendMessage(initialTopologyMessage.uuid, Collections.singletonList(newHostId));
            //sent new host message to all other hosts
            NewHostMessage newHostMessage = new NewHostMessage(newHostAddress, newHostId,
                    newHostRandom);
            reliabilityLayer.sendViewMessage(connectedHosts, newHostMessage);
            acknowledgeMap.sendMessage(newHostMessage.uuid, connectedHosts);
            //wait for all confirms
            while (!acknowledgeMap.isComplete(newHostMessage.uuid) || !acknowledgeMap.isComplete(initialTopologyMessage.uuid)) {
                try {
                    ConfirmViewChangeMessage confirmMessage = confirmBuffer.take();
                    if (confirmMessage.confirmedAction == initialTopologyMessage.messageType) {
                        acknowledgeMap.receiveAck(initialTopologyMessage.uuid, confirmMessage.senderUid, connectedHosts);
                    } else if (confirmMessage.confirmedAction == newHostMessage.messageType) {
                        acknowledgeMap.receiveAck(newHostMessage.uuid, confirmMessage.senderUid, connectedHosts);
                    } else {
                        logger.warn("Unexpected confirm for action " + confirmMessage.confirmedAction);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            RestartViewMessage restartViewMessage = new RestartViewMessage();
            sendBroadcastAndWaitConfirms(restartViewMessage);
            endViewFreeze();
        } else if (viewChangeList != null) { //you received a NEW_HOST message
            startConnection(newHostId, newHostAddress);
            reliabilityLayer.sendViewMessage(Collections.singletonList(newHostId),
                    new ConnectRequestMessage(clientUID));
        }
    }

    private ViewManagerMessage getMessage() throws InterruptedException {
        return (ViewManagerMessage) buffer.retrieveStable().payload;
    }

    /**
     * Called by FaultRecovery when the log threshold is reached, stops the sending of messages and starts the
     * stabilisation phase and the checkpointing (with doCheckpoint)
     */
    public void handleCheckpoint() {
        if(realViewManager.isEmpty()){
            startFreezeView();
            Checkpoint checkpointToSend = faultRecovery.doCheckpoint();
            CheckpointMessage checkpointMessage = new CheckpointMessage(checkpointToSend);
            sendBroadcastAndWaitConfirms(checkpointMessage);
            new Thread("logConditionChecker"){
                @Override
                public void run() {
                    faultRecovery.checkCondition();
                }
            }.start();
            RestartViewMessage restartViewMessage = new RestartViewMessage();
            sendBroadcastAndWaitConfirms(restartViewMessage);
            endViewFreeze();
        }
    }

    /**
     * Test method, used temporary to start the library functionality of this class
     * TODO remove and add proper server functionality
     */
    public void start() {
        try {
            logger.info("Starting host with address " + InetAddress.getLocalHost().getHostAddress() + ", " +
                    "random " + random + " and uuid " + clientUID);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        communicationLayer.startDiscoverySender(clientUID, random);
    }

    public UUID getClientUID() {
        return clientUID;
    }

    public List<UUID> getConnectedClients() {
        return connectedHosts;
    }

    public int getProcessID() {
        return processID;
    }

    private void endViewFreeze() {
        reliabilityLayer.startMessageSending();
        logger.trace("Connected clients: " + connectedHosts);
    }

    private void startConnection(UUID newHostId, InetAddress newHostAddress) {
        waitingHosts.add(newHostId);
        communicationLayer.initConnection(newHostAddress, newHostId);
    }

    private void startFreezeView() {
        reliabilityLayer.stopMessageSending();
        FreezeViewMessage freezeMessage = new FreezeViewMessage();
        sendBroadcastAndWaitConfirms(freezeMessage);
        reliabilityLayer.waitStabilization();
        assert confirmBuffer.isEmpty();
        logger.debug("Freeze view complete");
    }

    private void sendBroadcastAndWaitConfirms(ViewManagerMessage message) {
        AcknowledgeMap viewAckMap = new AcknowledgeMap();
        viewAckMap.sendMessage(message.uuid, connectedHosts);
        reliabilityLayer.sendViewMessage(connectedHosts, message);
        while (!viewAckMap.isComplete(message.uuid)) {
            try {
                ConfirmViewChangeMessage confirmMessage = confirmBuffer.take();
                if (confirmMessage.confirmedAction == message.messageType) {
                    viewAckMap.receiveAck(message.uuid, confirmMessage.senderUid, connectedHosts);
                } else {
                    logger.warn("Unexpected confirm for action " + confirmMessage.confirmedAction);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        logger.trace("Received all confirms for " + message.messageType);
    }

    public StablePriorityBlockingQueue<ReliabilityMessage> getBuffer() {
        return buffer;
    }
}
