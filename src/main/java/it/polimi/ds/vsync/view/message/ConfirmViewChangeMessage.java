package it.polimi.ds.vsync.view.message;

import java.util.UUID;

public class ConfirmViewChangeMessage extends ViewManagerMessage {

    public final UUID senderUid;

    public ConfirmViewChangeMessage(UUID senderUid) {
        super(ViewChangeType.CONFIRM);
        this.senderUid = senderUid;
    }
}
