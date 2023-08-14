package it.polimi.ds.vsync.view.message;

import it.polimi.ds.vsync.KnowledgeableMessage;
import it.polimi.ds.vsync.KnowledgeableMessageType;

public abstract class ViewManagerMessage extends KnowledgeableMessage {
    public final ViewChangeType messageType;

    public ViewManagerMessage(ViewChangeType messageType) {
        super(KnowledgeableMessageType.VIEW);
        this.messageType = messageType;
    }
}
