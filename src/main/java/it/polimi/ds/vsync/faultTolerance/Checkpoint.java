package it.polimi.ds.vsync.faultTolerance;

import java.util.List;

public class Checkpoint {

    private final int checkpointID;

    private final List<byte[]> messages;

    public Checkpoint(int checkpointID, List<byte[]> messages) {
        this.checkpointID = checkpointID;
        this.messages = messages;
    }

    public int getCheckpointID() {
        return checkpointID;
    }

    public List<byte[]> getMessages() {
        return messages;
    }

}
