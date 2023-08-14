package it.polimi.ds.vsync.view.message;

public class ConfirmViewChangeMessage extends ViewManagerMessage {
    public ConfirmViewChangeMessage() {
        super(ViewChangeType.CONFIRM);
    }
}
