package it.polimi.ds.vsync.view.message;

public enum ViewChangeType {
    INIT_VIEW("InitialTopologyMessage"),
    CONFIRM("ConfirmViewChangeMessage"),

    FREEZE_VIEW("FreezeViewMessage"),
    ADVERTISE("AdvertiseMessage");
    public final String className;

    ViewChangeType(String className) {
        this.className = className;
    }
}