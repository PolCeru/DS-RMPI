package it.polimi.ds.vsync.view;

import it.polimi.ds.communication.CommunicationLayer;
import it.polimi.ds.communication.message.DiscoveryMessage;
import it.polimi.ds.reliability.ReliabilityLayer;
import it.polimi.ds.vsync.VSyncLayer;
import it.polimi.ds.vsync.faultTolerance.FaultRecovery;
import it.polimi.ds.vsync.view.message.AdvertiseMessage;
import it.polimi.ds.vsync.view.message.ConfirmViewChangeMessage;
import it.polimi.ds.vsync.view.message.InitialTopologyMessage;
import it.polimi.ds.vsync.view.message.ViewManagerMessage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

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

    private Optional<UUID> realViewManager = Optional.empty();

    private final List<UUID> connectedHosts = new LinkedList<>();

    private final List<UUID> waitingHosts = new LinkedList<>();

    public ViewManager(VSyncLayer vSyncLayer, ReliabilityLayer reliabilityLayer,
                       CommunicationLayer communicationLayer, FaultRecovery faultRecovery){
        this.faultRecovery = faultRecovery;
        this.communicationLayer = communicationLayer;
        this.reliabilityLayer = reliabilityLayer;
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
        if (!isConnected) {
            if (random < newHostRandom) {
                if (realViewManager.isEmpty()) {
                    processID = 1;
                    clientsProcessIDCounter = 1;
                }
                waitingHosts.add(newHostId);
                communicationLayer.initConnection(newHostAddress, newHostId);
                communicationLayer.stopDiscoverySender();
                reliabilityLayer.sendViewMessage(Collections.singletonList(newHostId),
                        new InitialTopologyMessage(clientUID, clientsProcessIDCounter++, getCompleteTopology()));
            } else {
                System.out.println("New host:" + newHostAddress.getHostAddress() + "(random " + newHostRandom + ")");
            }
        } else if (realViewManager.isEmpty()) {
            //TODO: handle creation logic and propagation of the view
            processID++;
            clientsProcessIDCounter++;
        } else {
            reliabilityLayer.sendViewMessage(List.of(realViewManager.get()), new AdvertiseMessage(newHostAddress,
                    newHostId));
        }
    }

    private List<UUID> getCompleteTopology() {
        List<UUID> uuids = new ArrayList<>(connectedHosts);
        uuids.add(clientUID);
        return uuids;
    }

    /**
     * Test method, used temporary to start the library functionality of this class
     * TODO remove and add proper server functionality
     */
    public void start() {
        try {
            System.out.println("Starting host with address " + InetAddress.getLocalHost().getHostAddress() + ", " +
                    "random " + random + " and uuid " + clientUID);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        communicationLayer.startDiscoverySender(clientUID, random);
    }

    public void handleViewMessage(ViewManagerMessage baseMessage) {
        switch (baseMessage.messageType) {
            case CONFIRM -> {
                ConfirmViewChangeMessage message = (ConfirmViewChangeMessage) baseMessage;
                if (!isConnected) {
                    connectedHosts.add(message.senderUid);
                    isConnected = true;
                } else {
                    //TODO what action confirm this operation??
                }
            }
            case INIT_VIEW -> {
                InitialTopologyMessage message = (InitialTopologyMessage) baseMessage;
                if (!isConnected) {
                    realViewManager = Optional.of(message.viewManagerId);
                    if (message.topology.size() == 1) {
                        //only the viewManager connected
                        handleNewConnection(realViewManager.get());
                    } else {
                        //TODO implement method that create a waitingList with provided list,
                        // confirm that's ready to viewManager and wait for all connections
                    }
                } else {
                    //TODO manage the error or avoid?
                }
                //TODO: case RECOVER e RECOVERY_PACKET
            }
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
        if (!isConnected) {
            communicationLayer.stopDiscoverySender();
            isConnected = true;
            connectedHosts.add(newHostId);
        }
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
}
