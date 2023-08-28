package it.polimi.ds.lib.vsync.view.message;

public class FreezeViewMessage extends ViewManagerMessage {
    public FreezeViewMessage() {
        super(ViewChangeType.FREEZE_VIEW);
    }
}
