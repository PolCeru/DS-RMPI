package it.polimi.ds.vsync.view.message;

import it.polimi.ds.vsync.view.HostInfo;

import java.util.List;
import java.util.Objects;

public class InitialTopologyMessage extends ViewManagerMessage {
    final List<HostInfo> topology;

    public InitialTopologyMessage( List<HostInfo> topology) {
        super(ViewChangeType.INIT_VIEW);
        this.topology = topology;
    }

    public List<HostInfo> getTopology() {
        return topology;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InitialTopologyMessage that = (InitialTopologyMessage) o;
        return Objects.equals(topology, that.topology);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topology);
    }
}
