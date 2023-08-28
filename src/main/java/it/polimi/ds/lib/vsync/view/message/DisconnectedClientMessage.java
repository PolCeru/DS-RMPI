package it.polimi.ds.lib.vsync.view.message;

import java.util.UUID;

public class DisconnectedClientMessage extends ViewManagerMessage {
    final public UUID disconnectedClientUID;
    final public UUID newViewManagerUID;
    final public UUID newSubstituteViewManagerID;

    public DisconnectedClientMessage(UUID disconnectedClientUID) {
        super(ViewChangeType.DISCONNECTED_CLIENT);
        this.disconnectedClientUID = disconnectedClientUID;
        newSubstituteViewManagerID = null;
        newViewManagerUID = null;
    }

    public DisconnectedClientMessage(UUID disconnectedClientUID, UUID newViewManagerUID, UUID newSubstituteViewManagerID) {
        super(ViewChangeType.DISCONNECTED_CLIENT);
        this.disconnectedClientUID = disconnectedClientUID;
        this.newViewManagerUID = newViewManagerUID;
        this.newSubstituteViewManagerID = newSubstituteViewManagerID;
    }
}
