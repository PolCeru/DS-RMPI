package it.polimi.ds.vsync;

import it.polimi.ds.reliability.ReliabilityLayer;
import it.polimi.ds.vsync.faultTolerance.FaultRecovery;
import it.polimi.ds.vsync.view.ViewManager;
import it.polimi.ds.vsync.view.ViewManagerBuilder;

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
        //TODO remove - only for testing thread suspension
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.sendMessage(new VSyncMessage("test".getBytes()));
            }
        }, 10000, 4000);
    }

    public ReliabilityLayer getHandler() {
        return handler;
    }

    public ViewManager getViewManager() {
        return viewManager;
    }
}
