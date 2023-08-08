package it.polimi.ds.vsync.view.message;

public class ViewManagerMessage {
    private final ViewChangeType messageType;

    public ViewManagerMessage(ViewChangeType messageType) {
        this.messageType = messageType;
    }
}
