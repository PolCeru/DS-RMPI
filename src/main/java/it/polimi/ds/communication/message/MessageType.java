package it.polimi.ds.communication.message;

public enum MessageType {
    DATA("DataMessage"),
    DISCOVERY("DiscoveryMessage");

    public final String className;

    MessageType(String className) {
        this.className = className;
    }
}
