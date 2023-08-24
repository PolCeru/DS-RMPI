package it.polimi.ds.vsync.view.message;

import java.util.UUID;

public class DisconnectedClientMessage extends ViewManagerMessage {
    final public UUID disconnectedClientUID;

    public DisconnectedClientMessage(UUID disconnectedClientUID) {
        super(ViewChangeType.DISCONNECTED_CLIENT);
        this.disconnectedClientUID = disconnectedClientUID;
    }
}
