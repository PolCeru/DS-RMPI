package it.polimi.ds.vsync.view;

import it.polimi.ds.communication.CommunicationLayer;
import it.polimi.ds.communication.message.DiscoveryMessage;
import it.polimi.ds.reliability.ReliabilityLayer;
import it.polimi.ds.vsync.VSynchLayer;
import it.polimi.ds.vsync.view.message.AdvertiseMessage;
import it.polimi.ds.vsync.view.message.ViewManagerMessage;

import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;

public class ViewManager {

    /**
     * Random number used by the protocol to decide which device is the master and should start the connection
     */
    private final int random = new Random().nextInt();

    /**
     * Identify this machine uniquely
     */
    private final UUID uuid = UUID.randomUUID();

    private final CommunicationLayer communicationLayer;

    private final ReliabilityLayer reliabilityLayer;
    private boolean isConnected = false;

    private Optional<UUID> realViewManager = Optional.empty();

    private final List<UUID> connectedHosts = new LinkedList<>();

    public ViewManager(VSynchLayer vSynchLayer, ReliabilityLayer reliabilityLayer, CommunicationLayer communicationLayer) {
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
        if (connectedHosts.contains(newHostId) || newHostId.equals(uuid)) return;
        // first connection between devices
        if (!isConnected) {
            if (random < newHostRandom) {
                communicationLayer.initConnection(newHostAddress, newHostId, new DiscoveryMessage(LocalDateTime.now(), uuid, random));
                communicationLayer.stopDiscoverySender();
                connectedHosts.add(newHostId);
                isConnected = true;
            } else {
                System.out.println("DiscoveryMessage from " + newHostAddress.getHostAddress() + "(random " + newHostRandom + ")");
            }
        } else if (realViewManager.isEmpty()) {
            //TODO: handle creation logic and propagation of the view
        } else {
            reliabilityLayer.sendViewMessage(List.of(realViewManager.get()), new AdvertiseMessage(newHostAddress, newHostId));
        }
    }

    /**
     * Handle what happen when we receive a {@link DiscoveryMessage} over the TCP connection
     *
     * @param newHostId     the new host UUID
     * @param newHostRandom the new host random number
     * @param socket        the new host created by the TCP server when received the connection
     */
    public synchronized void handleNewConnection(UUID newHostId, int newHostRandom, Socket socket) {
        //already connected, so discard
        if (connectedHosts.contains(newHostId)) return;
        // first connection between device
        if (!isConnected) {
            //check correct master
            if (random >= newHostRandom) {
                communicationLayer.addClient(newHostId, socket);
                communicationLayer.stopDiscoverySender();
                isConnected = true;
                connectedHosts.add(newHostId);
            } else {
                throw new RuntimeException("Wrong connection starting: my random " + random + " vs " + newHostRandom);
            }
        }
    }

    /**
     * Test method, used temporary to start the library functionality of this class
     * TODO remove and add proper server functionality
     */
    public void start() {
        communicationLayer.startDiscoverySender(uuid, random);
    }

    public void handleViewMessage(ViewManagerMessage message) {
    }
}
