package it.polimi.ds.vsync;

import it.polimi.ds.reliability.ReliabilityLayer;

public class VSynchLayer {

    private final ReliabilityLayer handler;

    public VSynchLayer(ReliabilityLayer handler) {
        this.handler = handler;
    }
}
