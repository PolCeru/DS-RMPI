package it.polimi.ds.vsync;
import it.polimi.ds.reliability.KnowledgeableMessage;

public class VSyncMessage extends KnowledgeableMessage {

    private byte[] payload;

    public VSyncMessage(byte[] payload) {
        this.payload = payload;
    }
}