package it.polimi.ds.vsync.faultTolerance;

import java.util.ArrayList;

public class Checkpoint {

    private final int checkpointID;

    private final ArrayList<Byte[]> messages;

    public Checkpoint(int checkpointID, ArrayList<Byte[]> messages) {
        this.checkpointID = checkpointID;
        this.messages = messages;
    }

    public int getCheckpointID() {
        return checkpointID;
    }

    public ArrayList<Byte[]> getMessages() {
        return messages;
    }

}
