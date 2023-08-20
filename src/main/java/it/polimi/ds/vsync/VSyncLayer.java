package it.polimi.ds.vsync;

import it.polimi.ds.reliability.ReliabilityLayer;
import it.polimi.ds.vsync.faultTolerance.FaultRecovery;
import it.polimi.ds.vsync.view.ViewManager;
import it.polimi.ds.vsync.view.ViewManagerBuilder;

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
}
