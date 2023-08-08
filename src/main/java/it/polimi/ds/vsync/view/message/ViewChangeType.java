package it.polimi.ds.vsync.view.message;

public enum ViewChangeType {
    INIT_VIEW("InitialTopologyMessage");
    public final String className;

    ViewChangeType(String className) {
        this.className = className;
    }
}