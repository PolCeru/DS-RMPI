package it.polimi.ds.lib.vsync.view.message;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class InitialTopologyMessage extends ViewManagerMessage {
    public final List<UUID> topology;
    public final UUID viewManagerId;
    public final UUID substituteViewManagerId;
    public final int destinationProcessID;
    public final int checkpointCounter;

    public InitialTopologyMessage(UUID viewManagerId, int destinationProcessID, List<UUID> topology,
                                  UUID substituteViewManagerId, int checkpointCounter) {
        super(ViewChangeType.INIT_VIEW);
        this.viewManagerId = viewManagerId;
        this.topology = topology;
        this.destinationProcessID = destinationProcessID;
        this.substituteViewManagerId = substituteViewManagerId;
        this.checkpointCounter = checkpointCounter;
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
