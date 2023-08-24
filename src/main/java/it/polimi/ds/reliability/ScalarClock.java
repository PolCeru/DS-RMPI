package it.polimi.ds.reliability;

public record ScalarClock(int processID, int eventID) {
    @Override
    public String toString() {
        return "(" + eventID + "." + processID + ")";
    }
}
