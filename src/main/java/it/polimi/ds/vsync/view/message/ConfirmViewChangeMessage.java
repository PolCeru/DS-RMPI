package it.polimi.ds.vsync.view.message;

import java.util.UUID;

public class ConfirmViewChangeMessage extends ViewManagerMessage {

    public final UUID senderUid;
    public final ViewChangeType confirmedAction;

    public ConfirmViewChangeMessage(UUID senderUid, ViewChangeType confirmedAction) {
        super(ViewChangeType.CONFIRM);
        this.senderUid = senderUid;
        this.confirmedAction = confirmedAction;
    }
}
