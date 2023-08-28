package it.polimi.ds.lib.vsync.view.message;

public class RestartViewMessage extends ViewManagerMessage {
    public RestartViewMessage() {
        super(ViewChangeType.RESTART_VIEW);
    }
}
