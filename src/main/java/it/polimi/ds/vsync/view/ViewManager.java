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

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

//@SuppressWarnings("OptionalGetWithoutIsPresent")
public class ViewManager {

    /**
     * Random number used by the protocol to decide which device is the master and should start the connection
     */
    private final int random;

    /**
     * Identify this machine uniquely
     */
    private final UUID clientUID;

    private int processID;

    private int clientsProcessIDCounter;

    private final CommunicationLayer communicationLayer;

    private final ReliabilityLayer reliabilityLayer;

    private final FaultRecovery faultRecovery;

    private boolean isConnected = false;

    private boolean isRecoverable = false;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<UUID> realViewManager = Optional.empty();
    private Optional<UUID> substituteRealManager = Optional.empty();

    private final List<UUID> connectedHosts = new LinkedList<>();

    private final List<UUID> disconnectedHosts = new LinkedList<>();

    private final List<UUID> waitingHosts = new LinkedList<>();

    private final StablePriorityBlockingQueue<ReliabilityMessage> buffer = new StablePriorityBlockingQueue<>();

    private final BlockingQueue<ConfirmViewChangeMessage> confirmBuffer = new LinkedBlockingQueue<>();

    private ViewChangeList viewChangeList;

    private static final Logger logger = LogManager.getLogger();

    private final String FILE_PATH;

    private final String P_CLIENT_ID = "ClientID";

    private final String P_PROCESS_ID = "ProcessID";

    private final String P_RANDOM = "Random";

    private final Properties properties = new Properties();

    public ViewManager(VSyncLayer vSyncLayer, ReliabilityLayer reliabilityLayer, CommunicationLayer communicationLayer, FaultRecovery faultRecovery) {
        this.faultRecovery = faultRecovery;
        this.communicationLayer = communicationLayer;
        this.reliabilityLayer = reliabilityLayer;

        FILE_PATH = System.getProperty("user.home") + File.separator + "recovery" + File.separator +
                "recovery.txt";

        File file = new File(FILE_PATH);
        File directory = file.getParentFile();

        // Verifies whether directory exists, if not creates it
        if (!directory.exists()) {
            if (directory.mkdirs())
                logger.debug("Directory for file recovery created");
            else logger.error("Error creating directory");
        }

        if (!file.exists()) {
            try {
                if (file.createNewFile()) logger.debug("File for recovery created");
                else logger.debug("Impossible to create file");
            } catch (IOException e) {
                logger.fatal("Error creating file: " + e.getMessage());
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            properties.load(reader);
            isRecoverable = properties.containsKey(P_CLIENT_ID) &&
                    properties.containsKey(P_PROCESS_ID) &&
                    properties.containsKey(P_RANDOM) &&
                    disconnectedHosts.contains(UUID.fromString(properties.getProperty(P_CLIENT_ID)));
        } catch (FileNotFoundException e) {
            logger.debug("No recovery found in directory: " + FILE_PATH);
            logger.debug("Starting as new client");
        } catch (IOException e) {
            logger.fatal("There's an error with the BufferedReader " + e.getMessage());
        }

        if (isRecoverable) {
            this.clientUID = UUID.fromString(properties.getProperty(P_CLIENT_ID));
            this.random = Integer.parseInt(properties.getProperty(P_RANDOM));
        } else {
            this.clientUID = UUID.randomUUID();
            this.random = new Random().nextInt();
        }

        new Thread("ViewManager") {
            @Override
            public void run() {
                while (true) {
                    try {
                        ViewManagerMessage message = getMessage();
                        handleViewMessage(message);
                    } catch (InterruptedException e) {
                        logger.debug("ViewManager getMessage interrupted");
                    }
                }
            }
        }.start();
    }

    public void handleViewMessage(ViewManagerMessage baseMessage) {
        if (baseMessage.messageType != ViewChangeType.CONFIRM)
            logger.debug("Received stable message " + baseMessage.messageType + " with id: " + baseMessage.uuid);
        switch (baseMessage.messageType) {
            case CONFIRM -> {
                ConfirmViewChangeMessage message = (ConfirmViewChangeMessage) baseMessage;
                logger.debug("Received CONFIRM " + message.confirmedAction + " from " + message.senderUid);
                if (connectedHosts.isEmpty()) {
                    waitingHosts.remove(message.senderUid);
                    substituteRealManager = Optional.of(message.senderUid);
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
                        case DISCONNECTED_CLIENT -> //received by manager when a group member knows that someone disconnected
                                confirmBuffer.add(message);
                        case RECOVERY_PACKET -> endViewFreeze();
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
                        case RESTART_VIEW -> confirmBuffer.add(message);
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
                        if (message.destinationProcessID == -1 && isRecoverable)
                            processID = Integer.parseInt(properties.getProperty(P_PROCESS_ID));
                        else {
                            processID = message.destinationProcessID;
                            faultRecovery.setCheckpointCounter(message.checkpointCounter);
                        }
                        saveDataOnDisk(clientUID, processID, random);
                    } else {
                        if (message.substituteViewManagerId != clientUID) {
                            substituteRealManager = Optional.of(message.substituteViewManagerId);
                        }
                        if (viewChangeList == null) viewChangeList = ViewChangeList.fromExpectedUsers(message.topology);
                        else viewChangeList.setExpectedUsers(message.topology);
                        //add the view manager to the connected list
                        handleNewConnection(message.viewManagerId);
                        if (message.destinationProcessID == -1 && isRecoverable)
                            processID = Integer.parseInt(properties.getProperty(P_PROCESS_ID));
                        else{
                            processID = message.destinationProcessID;
                            faultRecovery.setCheckpointCounter(message.checkpointCounter);
                        }
                        saveDataOnDisk(clientUID, processID, random);
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
                reliabilityLayer.sendViewMessage(List.of(realViewManager.get()),
                        new ConfirmViewChangeMessage(clientUID, ViewChangeType.INIT_VIEW));
                //Requests missing checkpoints
                try (BufferedReader reader = new BufferedReader(new FileReader(faultRecovery.RECOVERY_FILE_PATH))) {
                    faultRecovery.getProperties().load(reader);
                    int checkpointCounter = Integer.parseInt(faultRecovery.getProperties().getProperty("CheckpointCounter"));
                    checkpointCounter = checkpointCounter > 0 ? checkpointCounter : -1;
                    reliabilityLayer.sendViewMessage(List.of(realViewManager.get()), new RecoveryRequestMessage(checkpointCounter));
                } catch (FileNotFoundException e) {
                    logger.debug("No recovery file found in directory");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case FREEZE_VIEW -> {
                //received by group member from manager when the view is frozen
                reliabilityLayer.stopMessageSending();
                reliabilityLayer.waitStabilization();
                logger.debug("Freeze view complete");
                reliabilityLayer.sendViewMessage(List.of(realViewManager.get()), new ConfirmViewChangeMessage(clientUID, ViewChangeType.FREEZE_VIEW));
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
                //received by group member from manager when the view is restarted
                reliabilityLayer.sendViewMessage(Collections.singletonList(realViewManager.get()),
                        new ConfirmViewChangeMessage(clientUID, ViewChangeType.RESTART_VIEW));
                endViewFreeze();
            }
            case CHECKPOINT -> {
                //received by group member from manager when a new checkpoint is created
                faultRecovery.doCheckpoint();
                reliabilityLayer.sendViewMessage(Collections.singletonList(realViewManager.get()),
                        new ConfirmViewChangeMessage(clientUID, ViewChangeType.CHECKPOINT));
            }
            case DISCONNECTED_CLIENT -> {
                //received by group member from manager when a client disconnects
                DisconnectedClientMessage disconnectedClientMessage = (DisconnectedClientMessage) baseMessage;
                if (disconnectedClientMessage.newViewManagerUID != null) {
                    realViewManager = Optional.of(disconnectedClientMessage.newViewManagerUID);
                    substituteRealManager = Optional.of(disconnectedClientMessage.newSubstituteViewManagerID);
                }
                handleDisconnection(disconnectedClientMessage.disconnectedClientUID);
                reliabilityLayer.sendViewMessage(Collections.singletonList(realViewManager.get()), new ConfirmViewChangeMessage(clientUID, ViewChangeType.DISCONNECTED_CLIENT));
            }
            case RECOVERY_REQUEST -> {
                //received by group member from manager when a client wants to retrieve missing checkpoints
                RecoveryRequestMessage message = (RecoveryRequestMessage) baseMessage;
                ArrayList<Checkpoint> checkpoints;
                if (message.lastCheckpointID >= 0) {
                    int checkpointID = message.lastCheckpointID;
                    checkpoints = faultRecovery.recoverCheckpoint(checkpointID);
                } else checkpoints = new ArrayList<>();
                reliabilityLayer.sendViewMessage(Collections.singletonList(realViewManager.get()),
                        new RecoveryPacketMessage(checkpoints));
            }
            case RECOVERY_PACKET -> {
                //received by group member from manager, it contains the missing checkpoints requested
                RecoveryPacketMessage message = (RecoveryPacketMessage) baseMessage;
                faultRecovery.addMissingCheckpoints(message.checkpoints);
                reliabilityLayer.sendViewMessage(Collections.singletonList(realViewManager.get()),
                        new ConfirmViewChangeMessage(clientUID, ViewChangeType.RECOVERY_PACKET));
            }
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
                    logger.error("I AM THE REAL MANAGER");
                    processID = 1;
                    clientsProcessIDCounter = 1;
                    saveDataOnDisk(clientUID, processID, random);
                }
                communicationLayer.stopDiscoverySender();
                startConnection(newHostId, newHostAddress);
                reliabilityLayer.sendViewMessage(Collections.singletonList(newHostId),
                        new InitialTopologyMessage(clientUID, ++clientsProcessIDCounter, getCompleteTopology(),
                                newHostId, faultRecovery.getCheckpointCounter()));
            } else {
                logger.info("New host:" + newHostAddress.getHostAddress() + "(random " + newHostRandom + ")");
            }
        } else if (realViewManager.isEmpty()) { //what to do when you are the real manager
            waitingHosts.add(newHostId);
            startFreezeView();
            handleCheckpoint();
            communicationLayer.initConnection(newHostAddress, newHostId);
            AcknowledgeMap acknowledgeMap = new AcknowledgeMap();
            InitialTopologyMessage initialTopologyMessage;
            //sent init view message to new host
            if (disconnectedHosts.contains(clientUID) && isRecoverable) {
                disconnectedHosts.remove(clientUID);
                connectedHosts.add(clientUID);
                initialTopologyMessage = new InitialTopologyMessage(clientUID, -1, getCompleteTopology(),
                        substituteRealManager.orElse(null), 0);
            } else
                initialTopologyMessage = new InitialTopologyMessage(clientUID, ++clientsProcessIDCounter,
                        getCompleteTopology(), substituteRealManager.orElse(null), faultRecovery.getCheckpointCounter());
            reliabilityLayer.sendViewMessage(Collections.singletonList(newHostId), initialTopologyMessage);
            acknowledgeMap.sendMessage(initialTopologyMessage.uuid, Collections.singletonList(newHostId));
            //sent new host message to all other hosts
            NewHostMessage newHostMessage = new NewHostMessage(newHostAddress, newHostId, newHostRandom);
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
            if (!isRecoverable) {
                RestartViewMessage restartViewMessage = new RestartViewMessage();
                sendBroadcastAndWaitConfirms(restartViewMessage);
                endViewFreeze();
            }
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
        if (realViewManager.isEmpty()) {
            faultRecovery.doCheckpoint();
            CheckpointMessage checkpointMessage = new CheckpointMessage();
            sendBroadcastAndWaitConfirms(checkpointMessage);
            new Thread("logConditionChecker") {
                @Override
                public void run() {
                    faultRecovery.checkCondition();
                }
            }.start();
        }
    }

    public void freezeAndCheckpoint() {
        if(realViewManager.isEmpty()){
            startFreezeView();
            handleCheckpoint();
            RestartViewMessage restartViewMessage = new RestartViewMessage();
            sendBroadcastAndWaitConfirms(restartViewMessage);
            endViewFreeze();
        }
    }

    /**
     * This method stops the sending of messages and starts the stabilisation phase and the checkpointing
     *
     * @param clientUID the UUID of the disconnected client
     */
    public synchronized void handleDisconnection(UUID clientUID) {
        boolean changeManager = false;
        if (connectedHosts.contains(clientUID)) {
            reliabilityLayer.stopMessageSending();
            connectedHosts.remove(clientUID);
            disconnectedHosts.add(clientUID);
            reliabilityLayer.handleDisconnection(clientUID);
        }
        if (clientUID.equals(realViewManager.orElse(null)) && substituteRealManager.isEmpty()) {
            realViewManager = Optional.empty();
            substituteRealManager = connectedHosts.stream().findFirst();
            changeManager = true;
        }
        if (realViewManager.isEmpty()) {
            DisconnectedClientMessage disconnectedClientMessage = changeManager ? new DisconnectedClientMessage(clientUID, this.clientUID, substituteRealManager.orElse(null)) : new DisconnectedClientMessage(clientUID);
            sendBroadcastAndWaitConfirms(disconnectedClientMessage);
            logger.info("disconnected client " + clientUID);
            handleCheckpoint();
        }
    }

    /**
     * Test method, used temporary to start the library functionality of this class
     * TODO remove and add proper server functionality
     */
    public void start() {
        try {
            logger.info("Starting host with address " + InetAddress.getLocalHost().getHostAddress() + ", " + "random " + random + " and uuid " + clientUID);
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
        logger.debug("Freeze view ended");
        logger.trace("Connected clients: " + connectedHosts);
    }

    private void startConnection(UUID newHostId, InetAddress newHostAddress) {
        waitingHosts.add(newHostId);
        communicationLayer.initConnection(newHostAddress, newHostId);
    }

    private void startFreezeView() {
        if(realViewManager.isEmpty()){
            reliabilityLayer.stopMessageSending();
            FreezeViewMessage freezeMessage = new FreezeViewMessage();
            sendBroadcastAndWaitConfirms(freezeMessage);
            reliabilityLayer.waitStabilization();
            assert confirmBuffer.isEmpty();
            logger.debug("Freeze view complete");
        }
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

    private void saveDataOnDisk(UUID clientUID, int processID, int random) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            properties.setProperty(P_CLIENT_ID, clientUID.toString());
            properties.setProperty(P_PROCESS_ID, String.valueOf(processID));
            properties.setProperty(P_RANDOM, String.valueOf(random));
            properties.store(writer, "Info to recover the client in case of disconnection");
            logger.info("Saved recovery data (ClientUID: " + clientUID + "; ProcessID: " + processID + "; Random: " + random + " into the disk");
        } catch (IOException e) {
            logger.fatal("Error writing recovery data to file: " + e.getMessage());
        }
    }

    public StablePriorityBlockingQueue<ReliabilityMessage> getBuffer() {
        return buffer;
    }
}
