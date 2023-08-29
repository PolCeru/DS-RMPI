package it.polimi.ds.lib.vsync.view;

import it.polimi.ds.lib.communication.CommunicationLayer;
import it.polimi.ds.lib.reliability.ReliabilityLayer;
import it.polimi.ds.lib.vsync.VSyncLayer;
import it.polimi.ds.lib.vsync.faultTolerance.FaultRecovery;

public class ViewManagerBuilder {
    final VSyncLayer vSyncLayer;

    final FaultRecovery faultRecovery;

    ReliabilityLayer reliabilityLayer = null;

    CommunicationLayer communicationLayer = null;

    ViewManager instance = null;

    public ViewManagerBuilder(VSyncLayer vSyncLayer, FaultRecovery faultRecovery) {
        this.faultRecovery = faultRecovery;
        this.vSyncLayer = vSyncLayer;
    }

    public void setReliabilityLayer(ReliabilityLayer reliabilityLayer) {
        if (this.reliabilityLayer == null) this.reliabilityLayer = reliabilityLayer;
    }

    public void setCommunicationLayer(CommunicationLayer communicationLayer) {
        if (this.communicationLayer == null) this.communicationLayer = communicationLayer;
    }

    public ViewManager create() {
        if (instance == null) instance = new ViewManager(vSyncLayer, reliabilityLayer, communicationLayer, faultRecovery);
        return instance;
    }

    public VSyncLayer getVSyncLayer() {
        return vSyncLayer;
    }
}
