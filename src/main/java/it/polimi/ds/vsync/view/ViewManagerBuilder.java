package it.polimi.ds.vsync.view;

import it.polimi.ds.communication.CommunicationLayer;
import it.polimi.ds.reliability.ReliabilityLayer;
import it.polimi.ds.vsync.VSynchLayer;

public class ViewManagerBuilder {
    final VSynchLayer vSynchLayer;

    ReliabilityLayer reliabilityLayer = null;

    CommunicationLayer communicationLayer = null;

    ViewManager instance = null;

    public ViewManagerBuilder(VSynchLayer vSynchLayer) {
        this.vSynchLayer = vSynchLayer;
    }

    public void setReliabilityLayer(ReliabilityLayer reliabilityLayer) {
        if (this.reliabilityLayer == null) this.reliabilityLayer = reliabilityLayer;
    }

    public void setCommunicationLayer(CommunicationLayer communicationLayer) {
        if (this.communicationLayer == null) this.communicationLayer = communicationLayer;
    }

    public ViewManager create() {
        if (instance == null) instance = new ViewManager(vSynchLayer, reliabilityLayer, communicationLayer);
        return instance;
    }
}
