package it.polimi.ds.vsync.faultTolerance;

import java.util.ArrayList;

public class Checkpoint {

    private final int checkpointID;

    private final ArrayList<byte[]> messages;

    public Checkpoint(int checkpointID, ArrayList<byte[]> messages) {
        this.checkpointID = checkpointID;
        this.messages = messages;
    }

    public int getCheckpointID() {
        return checkpointID;
    }

    public ArrayList<byte[]> getMessages() {
        return messages;
    }

}
