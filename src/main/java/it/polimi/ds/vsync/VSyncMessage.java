package it.polimi.ds.vsync;

public class VSyncMessage extends KnowledgeableMessage {
    byte[] payload;

    public VSyncMessage(byte[] payload) {
        super(KnowledgeableMessageType.VSYNC);
        this.payload = payload;
    }
}
