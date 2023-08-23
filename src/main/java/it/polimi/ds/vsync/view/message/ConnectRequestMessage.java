package it.polimi.ds.vsync.view.message;

import java.util.UUID;

public class ConnectRequestMessage extends ViewManagerMessage {
    final public UUID senderUid;

    public ConnectRequestMessage(UUID senderUid) {
        super(ViewChangeType.CONNECT_REQ);
        this.senderUid = senderUid;
    }
}
