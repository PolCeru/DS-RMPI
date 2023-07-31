package it.polimi.ds.communication;

public enum MessageType {
    DATA("DataMessage"),
    ACK("AckMessage"),
    DISCOVERY("DiscoveryMessage");

    private final String className;

    MessageType(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
