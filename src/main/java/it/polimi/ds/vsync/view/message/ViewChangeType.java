package it.polimi.ds.vsync.view.message;

public enum ViewChangeType {
    INIT_VIEW("InitialTopologyMessage"),
    CONFIRM("ConfirmViewChangeMessage"),

    FREEZE_VIEW("FreezeViewMessage"),
    ADVERTISE("AdvertiseMessage"),
    CONNECT_REQ("ConnectRequestMessage"),
    NEW_HOST("NewHostMessage"),
    RESTART_VIEW("RestartViewMessage");
    public final String className;

    ViewChangeType(String className) {
        this.className = className;
    }
}