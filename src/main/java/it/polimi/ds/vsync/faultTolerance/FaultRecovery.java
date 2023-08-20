package it.polimi.ds.vsync.faultTolerance;

import com.google.gson.Gson;
import it.polimi.ds.vsync.VSyncLayer;
import it.polimi.ds.vsync.VSyncMessage;

import java.util.ArrayList;

public class FaultRecovery {
    private final VSyncLayer vSyncLayer;

    private final ArrayList<Byte[]> log = new ArrayList<>();

    private final ArrayList<Checkpoint> checkpoints = new ArrayList<>();

    private int checkpointCounter = 0;

    private final Gson gson = new Gson();

    private final int LOG_TRESHOLD = 1024;

    public FaultRecovery(VSyncLayer vSyncLayer) {
        this.vSyncLayer = vSyncLayer;
    }

    /**
     * This method creates a new checkpoint to be added in the list and increments the checkpoint counter; in case
     * the log in the recoveryPacket is not empty it adds the messages in the log in the correct order
     * @param recoveryPacket the recovery packet containing the checkpoint and the log
     */
    public void initFaultRecovery(RecoveryPacket recoveryPacket){
        //TODO: implement
    }

    /**
     * This method creates a new checkpoint writing it into disk and emptying the log; then adds it to the list of
     * checkpoints incrementing the counter
     */
    public void doCheckpoint(){
        //TODO: implement
    }

    /**
     * This method search for the requested checkpoint and returns every checkpoint after it in the list (including
     * it) and the current content of the log in a recovery packet
     * @param checkpointID
     * @return
     */
    public RecoveryPacket recoverCheckpoint(int checkpointID){
        //TODO: implement
        return null;
    }

    /**
     * This method adds the message to the log and if the log is full starts the checkpoint procedure
     * @param message the message to be added to the log
     */
    public void logMessage(VSyncMessage message){
        //TODO: implement
    }
}
