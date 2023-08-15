package it.polimi.ds.vsync;

public abstract class KnowledgeableMessage {
    public final KnowledgeableMessageType knowledgeableMessageType;

    protected KnowledgeableMessage(KnowledgeableMessageType knowledgeableMessageType) {
        this.knowledgeableMessageType = knowledgeableMessageType;
    }
}
