package it.polimi.ds.lib.vsync;

public abstract class KnowledgeableMessage {
    public final KnowledgeableMessageType knowledgeableMessageType;

    protected KnowledgeableMessage(KnowledgeableMessageType knowledgeableMessageType) {
        this.knowledgeableMessageType = knowledgeableMessageType;
    }
}
