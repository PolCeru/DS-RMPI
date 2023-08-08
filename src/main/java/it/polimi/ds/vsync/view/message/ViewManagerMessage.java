package it.polimi.ds.vsync.view.message;

import it.polimi.ds.reliability.KnowledgeableMessage;

public abstract class ViewManagerMessage extends KnowledgeableMessage {
    private final ViewChangeType messageType;

    public ViewManagerMessage(ViewChangeType messageType) {
        this.messageType = messageType;
    }
}
