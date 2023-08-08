package it.polimi.ds.vsync.view;

import it.polimi.ds.communication.CommunicationLayer;

import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ViewManager {
    private final CommunicationLayer communicationLayer;
    private boolean isConnected = false;

    private Optional<UUID> realViewManager = Optional.empty();

    private final List<UUID> connectedHosts = new LinkedList<>();

    public ViewManager(CommunicationLayer communicationLayer) {
        this.communicationLayer = communicationLayer;
    }

    public synchronized void handleNewHost(UUID newHostId, int newHostRandom, InetAddress newHostAddress) {
        // already connected, so discard
        if (connectedHosts.contains(newHostId)) return;
        // first connection between devices
        if (!isConnected) {
            if (communicationLayer.getRandom() < newHostRandom) {
                communicationLayer.initConnection(newHostAddress, newHostRandom, newHostId);
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
            if (communicationLayer.getRandom() >= newHostRandom) {
                communicationLayer.addClient(newHostId, socket);
                communicationLayer.stopDiscoverySender();
                isConnected = true;
                connectedHosts.add(newHostId);
            } else {
                throw new RuntimeException("Wrong connection starting: my random " + communicationLayer.getRandom() + " vs " + newHostRandom);
            }
        }
    }
}
