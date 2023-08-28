package it.polimi.ds.lib.utils;

import it.polimi.ds.lib.utils.StablePriorityBlockingQueue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StablePriorityBlockingQueueTest {
    //test all method of StablePriorityBlockingQueue
    @Test
    void testAddWithSize() {
        StablePriorityBlockingQueue<Integer> queue = new StablePriorityBlockingQueue<>();
        queue.add(1);
        queue.add(2);
        queue.add(3);
        assertEquals(3, queue.size());
    }

    @Test
    void testAddWithRetrieveStable() throws InterruptedException {
        StablePriorityBlockingQueue<Integer> queue = new StablePriorityBlockingQueue<>();
        queue.add(1);
        queue.add(2);
        queue.add(3);
        queue.markStable(1);
        queue.markStable(2);
        queue.markStable(3);
        assertEquals(1, queue.retrieveStable());
        assertEquals(2, queue.retrieveStable());
        assertEquals(3, queue.retrieveStable());
    }

    @Test
    void testOrderRetrievalWithUnorderedAdd() throws InterruptedException {
        StablePriorityBlockingQueue<Integer> queue = new StablePriorityBlockingQueue<>();
        queue.add(3);
        queue.add(1);
        queue.add(2);
        queue.markStable(2);
        queue.markStable(3);
        queue.markStable(1);
        assertEquals(1, queue.retrieveStable());
        assertEquals(2, queue.retrieveStable());
        assertEquals(3, queue.retrieveStable());
    }

    @Test
    void testRetrieveOnEmptyQueueWaitIndefinetly() throws InterruptedException {
        Thread thread = new Thread(() -> {
            StablePriorityBlockingQueue<Integer> queue = new StablePriorityBlockingQueue<>();
            try {
                queue.retrieveStable();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
        Thread.sleep(2000);
        assertTrue(thread.isAlive());
    }

    @Test
    void testRetrieveOnNotStableHead() throws InterruptedException {
        Thread thread = new Thread(() -> {
            StablePriorityBlockingQueue<Integer> queue = new StablePriorityBlockingQueue<>();
            queue.add(1);
            queue.add(2);
            queue.add(3);
            queue.markStable(2);
            queue.markStable(3);
            try {
                queue.retrieveStable();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();
        Thread.sleep(2000);
        assertTrue(thread.isAlive());
    }
}