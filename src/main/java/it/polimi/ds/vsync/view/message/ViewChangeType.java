package it.polimi.ds.vsync.view.message;

public enum ViewChangeType {
    INIT_VIEW("InitialTopologyMessage"),
    CONFIRM("ConfirmViewChangeMessage"),
    CHECKPOINT("CheckpointMessage"),
    RECOVERY_REQUEST("RecoveryRequestMessage"),
    RECOVERY_PACKET("RecoveryPacketMessage"),
    FREEZE_VIEW("FreezeViewMessage"),
    CONNECT_REQ("ConnectRequestMessage"),
    DISCONNECTED_CLIENT("DisconnectedClientMessage"),
    NEW_HOST("NewHostMessage"),
    RESTART_VIEW("RestartViewMessage");
    public final String className;

    ViewChangeType(String className) {
        this.className = className;
    }
}