package it.polimi.ds.lib.reliability;

public record ScalarClock(int processID, int eventID) implements Comparable<ScalarClock> {
    @Override
    public String toString() {
        return "(" + eventID + "." + processID + ")";
    }

    @Override
    public int compareTo(ScalarClock o) {
        if (eventID() < o.eventID()) {
            return -1;
        } else if (eventID() > o.eventID()) {
            return +1;
        } else {
            return Integer.compare(processID(), o.processID());
        }
    }
}
