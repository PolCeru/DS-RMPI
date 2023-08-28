package it.polimi.ds.lib;

import it.polimi.ds.lib.vsync.VSyncLayer;

public class Middleware implements MiddlewareAPI {
    VSyncLayer vSyncLayer;
    @Override
    public void start() {
        vSyncLayer = new VSyncLayer();
    }

    @Override
    public void sendMessage(byte[] payload) {
        vSyncLayer.sendMessage(payload);
    }

    @Override
    public byte[] retrieveStableMessage() {
        return vSyncLayer.receiveMessage();
    }
}
