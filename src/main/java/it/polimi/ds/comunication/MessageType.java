package it.polimi.ds.comunication;

public enum MessageType {
    DATA("DataMessage"),
    DISCOVERY("DiscoveryMessage");

    private final String className;

    MessageType(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
