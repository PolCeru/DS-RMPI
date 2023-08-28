package it.polimi.ds.lib.vsync;

public enum KnowledgeableMessageType {
    VIEW("view.message.ViewManagerMessage"),
    VSYNC("VSyncMessage");
    public final String className;

    KnowledgeableMessageType(String className) {
        this.className = className;
    }
}
