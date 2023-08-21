package it.polimi.ds.vsync.view.message;

public class FreezeViewMessage extends ViewManagerMessage {
    public FreezeViewMessage() {
        super(ViewChangeType.FREEZE_VIEW);
    }
}
