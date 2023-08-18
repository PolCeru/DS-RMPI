package it.polimi.ds.vsync.view;

import it.polimi.ds.communication.CommunicationLayer;
import it.polimi.ds.reliability.ReliabilityLayer;
import it.polimi.ds.vsync.VSyncLayer;

public class ViewManagerBuilder {
    final VSyncLayer vSyncLayer;

    ReliabilityLayer reliabilityLayer = null;

    CommunicationLayer communicationLayer = null;

    ViewManager instance = null;

    public ViewManagerBuilder(VSyncLayer vSyncLayer) {
        this.vSyncLayer = vSyncLayer;
    }

    public void setReliabilityLayer(ReliabilityLayer reliabilityLayer) {
        if (this.reliabilityLayer == null) this.reliabilityLayer = reliabilityLayer;
    }

    public void setCommunicationLayer(CommunicationLayer communicationLayer) {
        if (this.communicationLayer == null) this.communicationLayer = communicationLayer;
    }

    public ViewManager create() {
        if (instance == null) instance = new ViewManager(vSyncLayer, reliabilityLayer, communicationLayer);
        return instance;
    }
}
