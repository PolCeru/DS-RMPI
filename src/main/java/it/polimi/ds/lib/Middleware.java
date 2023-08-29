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
    public byte[] readMessage() {
        return vSyncLayer.receiveMessage();
    }

    @Override
    public byte[] nextStableMessage() {
        return vSyncLayer.retrieveStableMessage();
    }
}
