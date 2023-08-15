package it.polimi.ds.vsync.view.message;

public class ViewChangeMessage extends ViewManagerMessage {
    public ViewChangeMessage() {
        super(ViewChangeType.VIEW_CHANGE);
    }
}
