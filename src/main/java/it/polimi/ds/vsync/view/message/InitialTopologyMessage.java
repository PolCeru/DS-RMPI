package it.polimi.ds.vsync.view.message;

import it.polimi.ds.vsync.view.HostInfo;

import java.util.List;

public class InitialTopologyMessage extends ViewManagerMessage {
    final List<HostInfo> topology;

    public InitialTopologyMessage(ViewChangeType type, List<HostInfo> topology) {
        super(type);
        this.topology = topology;
    }

    public List<HostInfo> getTopology() {
        return topology;
    }
}
