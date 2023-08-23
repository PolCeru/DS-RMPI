package it.polimi.ds.vsync.view.message;

public class RestartViewMessage extends ViewManagerMessage {
    public RestartViewMessage() {
        super(ViewChangeType.RESTART_VIEW);
    }
}
