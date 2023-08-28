package it.polimi.ds.lib.vsync;

import it.polimi.ds.lib.vsync.view.ViewManager;
import it.polimi.ds.lib.vsync.view.ViewManagerBuilder;
import it.polimi.ds.lib.reliability.ReliabilityLayer;
import it.polimi.ds.lib.vsync.faultTolerance.FaultRecovery;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class VSyncLayer {

    private final ReliabilityLayer handler;

    private final ViewManager viewManager;

    private final FaultRecovery faultRecovery;

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
}
