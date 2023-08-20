package it.polimi.ds.utils;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class StablePriorityBlockingQueue<E extends Comparable<E>> {
    private final BlockingQueue<Envelope> queue;
    private final ReentrantLock lock;
    private final Condition stableCondition;

    public StablePriorityBlockingQueue() {
        this.queue = new PriorityBlockingQueue<>();
        this.lock = new ReentrantLock();
        this.stableCondition = lock.newCondition();
    }

    public void add(E element) {
        lock.lock();
        try {
            Envelope envelope = new Envelope(element);
            queue.add(envelope);
            stableCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void markStable(E element) {
        lock.lock();
        try {
            if (!queue.remove(new Envelope(element))) {
                System.err.println("Marking as stable a message not present");
            }
            queue.add(new Envelope(element, true));
            stableCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public E retrieveStable() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty() || !queue.peek().stable) {
                // Wait for the head element to become stable or for the queue to have elements
                stableCondition.await();
            }
            return queue.poll().element;
        } finally {
            lock.unlock();
        }
    }

    // Additional methods for thread-safe access to queue size and element stability
    public int size() {
        return queue.size();
    }

    private class Envelope implements Comparable<Envelope> {
        final E element;
        boolean stable;

        private Envelope(E element) {
            this.element = element;
            this.stable = false;
        }

        private Envelope(E element, boolean stable) {
            this.stable = stable;
            this.element = element;
        }

        @Override
        public int hashCode() {
            return Objects.hash(element);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            //noinspection unchecked
            Envelope envelope = (Envelope) o;
            return Objects.equals(element, envelope.element);
        }

        @Override
        public int compareTo(Envelope o) {
            return element.compareTo(o.element);
        }
    }
}