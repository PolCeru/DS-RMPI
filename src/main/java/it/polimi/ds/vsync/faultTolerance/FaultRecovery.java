package it.polimi.ds.vsync.faultTolerance;

import com.google.gson.Gson;
import it.polimi.ds.vsync.VSyncLayer;
import it.polimi.ds.vsync.VSyncMessage;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

public class FaultRecovery {
    private final VSyncLayer vSyncLayer;

    private final ArrayList<byte[]> log = new ArrayList<>();

    private final ArrayList<Checkpoint> checkpoints = new ArrayList<>();

    private int checkpointCounter = 0;

    private final Gson gson = new Gson();

    private final int LOG_TRESHOLD = 1024;

    private final String filePath = "checkpoints/Checkpoint" + checkpointCounter + ".bin";

    public FaultRecovery(VSyncLayer vSyncLayer) {
        this.vSyncLayer = vSyncLayer;
    }

    /**
     * This method adds the checkpoints received in the list and updates the checkpoint counter.
     * Then proceeds to write the checkpoints into disk.
     * in case the log in the checkpointsToAdd is not empty it adds the messages in the log in the correct order
     * @param checkpointsToAdd the recovery packet containing the checkpoint and the log
     */
    public void addCheckpoints(ArrayList<Checkpoint> checkpointsToAdd){
        checkpoints.addAll(checkpointsToAdd);
        checkpoints.sort(Comparator.comparingInt(Checkpoint::getCheckpointID));
        checkpointCounter = Math.max(checkpoints.get(checkpoints.size() - 1).getCheckpointID(), checkpointCounter) + 1;

        checkpointsToAdd.forEach(checkpoint -> writeCheckpointOnFile(checkpoint.getMessages()));
        log.clear();
    }

    /**
     * This method creates a new checkpoint writing it into disk and emptying the log; then adds it to the list of
     * checkpoints incrementing the counter
     */
    public void doCheckpoint(){
        Checkpoint checkpoint = new Checkpoint(checkpointCounter, log);
        writeCheckpointOnFile(log);
        checkpointCounter++;
        checkpoints.add(checkpoint);
        log.clear();
    }

    /**
     * This method search for the requested checkpoint and returns every checkpoint after it in the list (including
     * it) and the current content of the log in a recovery packet
     * @param checkpointID the checkpoint to be recovered
     * @return the recovery packet containing the requested checkpoints and the log
     */
    public ArrayList<Checkpoint> recoverCheckpoint(int checkpointID){
        for (Checkpoint checkpoint : checkpoints) {
            if(checkpoint.getCheckpointID() == checkpointID){
                ArrayList<Checkpoint> checkpointsToReturn = new ArrayList<>();
                for (int i = checkpointCounter; i < checkpoints.size(); i++) {
                    checkpointsToReturn.add(checkpoints.get(i));
                }
                return new ArrayList<>(checkpointsToReturn);
            } else throw new IllegalArgumentException("Checkpoint not found");
        }
        return null;
    }

    /**
     * This method adds the message to the log and if the log is full starts the checkpoint procedure
     * @param message the message to be added to the log
     */
    public void logMessage(VSyncMessage message){
        log.add(gson.toJson(message).getBytes());
        if(log.size() >= LOG_TRESHOLD){
            vSyncLayer.getViewManager().handleCheckpoint();
        }
    }

    /**
     * This method writes the checkpoints on the disk
     * @param whatToWrite the checkpoints to be written in form of list of byte array
     */
    private void writeCheckpointOnFile(ArrayList<byte[]> whatToWrite) {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            for (byte[] row: whatToWrite)
                bos.write(row);
        } catch (IOException e) {
            System.err.println("Error writing checkpoint to file\n" + e.getMessage());
            e.printStackTrace();
        }
    }
}
