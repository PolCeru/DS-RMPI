package it.polimi.ds.lib.vsync.view.message;

import it.polimi.ds.lib.vsync.KnowledgeableMessage;
import it.polimi.ds.lib.vsync.KnowledgeableMessageType;

import java.util.UUID;

public abstract class ViewManagerMessage extends KnowledgeableMessage {
    public final ViewChangeType messageType;

    public final UUID uuid = UUID.randomUUID();

    public ViewManagerMessage(ViewChangeType messageType) {
        super(KnowledgeableMessageType.VIEW);
        this.messageType = messageType;
    }
}
