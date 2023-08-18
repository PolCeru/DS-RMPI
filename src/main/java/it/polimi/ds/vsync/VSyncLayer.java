package it.polimi.ds.vsync;

import it.polimi.ds.reliability.ReliabilityLayer;
import it.polimi.ds.vsync.view.ViewManager;
import it.polimi.ds.vsync.view.ViewManagerBuilder;

public class VSyncLayer {

    private final ReliabilityLayer handler;

    private final ViewManager viewManager;

    public VSyncLayer() {
        ViewManagerBuilder viewManagerBuilder = new ViewManagerBuilder(this);
        this.handler = new ReliabilityLayer(viewManagerBuilder);
        this.viewManager = viewManagerBuilder.create();
        viewManager.start();
    }

    public ReliabilityLayer getHandler() {
        return handler;
    }
}
