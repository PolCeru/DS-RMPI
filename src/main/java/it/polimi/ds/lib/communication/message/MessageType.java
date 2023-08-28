package it.polimi.ds.lib.communication.message;

/**
 * This enum is used to identify the type of message in the communicationLayer
 */
public enum MessageType {
    DATA("DataMessage"),
    DISCOVERY("DiscoveryMessage");
    public final String className;
    MessageType(String className) {
        this.className = className;
    }
}
