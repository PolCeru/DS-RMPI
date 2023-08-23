package it.polimi.ds.vsync.faultTolerance;

import com.google.gson.Gson;
import it.polimi.ds.vsync.VSyncLayer;
import it.polimi.ds.vsync.VSyncMessage;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FaultRecovery {
    private final VSyncLayer vSyncLayer;

    private final ArrayList<byte[]> log = new ArrayList<>();

    private final ArrayList<Checkpoint> checkpoints = new ArrayList<>();

    private int checkpointCounter = 0;

    private final Gson gson = new Gson();

    private final ReentrantLock lock;
    
    private final Condition thresholdCondition;
    
    private final int LOG_THRESHOLD = 1024;

    private final String FILE_PATH = "checkpoints/Checkpoint" + checkpointCounter + ".bin";

    public FaultRecovery(VSyncLayer vSyncLayer) {
        this.vSyncLayer = vSyncLayer;
        this.lock = new ReentrantLock();
        this.thresholdCondition = lock.newCondition();
        new Thread(this::checkCondition, "logConditionChecker").start();
        //TODO: crea un thread che è sempre in ascolto di chiamata a "logMessage" (=notify) e aspetta (wait) che il log
        // sia pieno e dopo di che chiama handleCheckpoint del viewManager
        // quando il checkpoint è finito fai ripartire il thread (con una flag o restart thread)
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
     * @return the created checkpoint
     */
    public Checkpoint doCheckpoint(){
        Checkpoint checkpoint = new Checkpoint(checkpointCounter, log);
        writeCheckpointOnFile(log);
        checkpoints.add(checkpoint);
        log.clear();
        System.out.println("Checkpoint " + (checkpointCounter) + " created successfully");
        System.out.println("Log cleared after checkpoint " + (checkpointCounter));
        checkpointCounter++;
        return checkpoint;
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
        lock.lock();
        try {
            log.add(gson.toJson(message).getBytes());
            thresholdCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * This method writes the checkpoints on the disk
     * @param whatToWrite the checkpoints to be written in form of list of byte array
     */
    private void writeCheckpointOnFile(ArrayList<byte[]> whatToWrite) {
        System.out.println("Writing checkpoint " + checkpointCounter + " to file");
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(FILE_PATH))) {
            for (byte[] row: whatToWrite)
                bos.write(row);
        } catch (IOException e) {
            System.err.println("Error writing checkpoint to file\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void checkCondition(){
        lock.lock();
        try {
            while (log.size() < LOG_THRESHOLD) {
                // Wait for the log to reach the threshold to call handleCheckpoint
                thresholdCondition.await();
            }
            vSyncLayer.getViewManager().handleCheckpoint();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
