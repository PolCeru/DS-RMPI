package it.polimi.ds.vsync.view;

import it.polimi.ds.communication.CommunicationLayer;
import it.polimi.ds.communication.message.DiscoveryMessage;
import it.polimi.ds.reliability.ReliabilityLayer;
import it.polimi.ds.vsync.VSynchLayer;

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
    private boolean isConnected = false;

    private Optional<UUID> realViewManager = Optional.empty();

    private final List<UUID> connectedHosts = new LinkedList<>();

    public ViewManager(VSynchLayer vSynchLayer, ReliabilityLayer reliabilityLayer, CommunicationLayer communicationLayer) {
        this.communicationLayer = communicationLayer;
    }

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
            //TODO: send AdvertiseMessage to the view manager
        }
    }

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

    public void start() {
        communicationLayer.startDiscoverySender(uuid, random);
    }
}
