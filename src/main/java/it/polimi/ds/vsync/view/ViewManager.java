package it.polimi.ds.vsync.view;

import it.polimi.ds.communication.CommunicationLayer;
import it.polimi.ds.communication.DiscoveryMessage;
import it.polimi.ds.vsync.VSynchLayer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ViewManager extends VSynchLayer {
    private final CommunicationLayer communicationLayer;
    private boolean isConnected = false;

    private Optional<UUID> realViewManager = Optional.empty();

    private final List<UUID> list = new LinkedList<>();

    public ViewManager(CommunicationLayer communicationLayer) {
        this.communicationLayer = communicationLayer;
    }

    public synchronized void handleNewHost(UUID newHostId, int newHostRandom, InetAddress newHostAddress) {
        // already connected, so discard
        if (list.contains(newHostId)) return;
        // first connection between devices
        if (!isConnected) {
            if (communicationLayer.getRandom() < newHostRandom) {
                communicationLayer.initConnection(newHostAddress, newHostRandom, newHostId);
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
        if (list.contains(newHostId)) return;
        // first connection between device
        if (!isConnected) {
            //check correct master
            if (communicationLayer.getRandom() >= newHostRandom) {
                communicationLayer.addClient(newHostId, socket);
            } else {
                throw new RuntimeException("Wrong connection starting: my random " + communicationLayer.getRandom() + " vs " + newHostRandom);
            }
        }
    }
}
