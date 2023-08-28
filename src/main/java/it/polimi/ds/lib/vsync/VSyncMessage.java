package it.polimi.ds.lib.vsync;

public class VSyncMessage extends KnowledgeableMessage {
    public byte[] payload;

    public VSyncMessage(byte[] payload) {
        super(KnowledgeableMessageType.VSYNC);
        this.payload = payload;
    }
}
