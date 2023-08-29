package it.polimi.ds.lib.vsync;

import it.polimi.ds.lib.reliability.ReliabilityMessage;
import it.polimi.ds.lib.vsync.view.ViewManager;
import it.polimi.ds.lib.vsync.view.ViewManagerBuilder;
import it.polimi.ds.lib.reliability.ReliabilityLayer;
import it.polimi.ds.lib.vsync.faultTolerance.FaultRecovery;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class VSyncLayer {

    private final ReliabilityLayer handler;

    private final ViewManager viewManager;

    private final FaultRecovery faultRecovery;

    final PriorityBlockingQueue<FaultRecovery.VSyncWrapper> buffer = new PriorityBlockingQueue<>();

    public VSyncLayer() {
        faultRecovery = new FaultRecovery(this);
        ViewManagerBuilder viewManagerBuilder = new ViewManagerBuilder(this, faultRecovery);
        this.handler = new ReliabilityLayer(viewManagerBuilder, faultRecovery);
        this.viewManager = viewManagerBuilder.create();
        viewManager.start();
    }

    public ReliabilityLayer getHandler() {
        return handler;
    }

    public ViewManager getViewManager() {
        return viewManager;
    }

    public byte[] receiveMessage() {
        return ((VSyncMessage) handler.getMessage().payload).payload;
    }

    public void sendMessage(byte[] payload) {
        handler.sendMessage(new VSyncMessage(payload));
    }

    public void addMessage(ReliabilityMessage message) {
        buffer.add(new FaultRecovery.VSyncWrapper((VSyncMessage) message.payload,message.timestamp ));
    }

    public void addAllMessage(List<FaultRecovery.VSyncWrapper> messages) {
        buffer.addAll(messages);
    }

    public byte[] retrieveStableMessage() {
        try {
            return ((VSyncMessage) buffer.take().message()).payload;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
