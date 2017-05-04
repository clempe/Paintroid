package org.catrobat.paintroid.test.junit;

import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.catrobat.paintroid.datastructures.LimitedSizeQueue;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.LinkedList;
import java.util.NoSuchElementException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class LimitedSizeQueueTest {

    private LimitedSizeQueue<Integer> queue;
    private final int limit = 5;

    @Before
    public void setUp() {
         queue = new LimitedSizeQueue<>(limit);
    }

    @Test
    public void testSizeZeroElements() {
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testSizeOverLimit() {
        for (int i = 0; i < limit*2; i++) {
            queue.add(i);
        }
        assertEquals(limit, queue.size());
    }

    @Test(expected = NoSuchElementException.class)
    public void testEmptyQueueGetYoungest() {
        queue.getYoungest();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithInvalidMaximum() {
        LimitedSizeQueue<Object> objectLimitedSizeQueue = new LimitedSizeQueue<>(0);
    }

    @Test(expected = NoSuchElementException.class)
    public void testEmptyQueueGetOldest() {
        queue.getOldest();
    }

    @Test
    public void testSizeWithLimitElements() {
        LinkedList<Integer> list = new LinkedList<>();

        for (int i = 0; i < limit; i++) {
            queue.add(i);
            list.add(i);
        }

        assertEquals(limit, queue.size());
        assertTrue(queue.isFull());

        assertEquals(list.peekLast(), queue.getYoungest());
        assertEquals(list.peekFirst(), queue.getOldest());

        for (int i = 0; i < limit; i++) {
            assertEquals(list.pollLast(), queue.pop());
        }
        assertEquals(null, queue.pop());
    }
    @Test

    public void testSizeWithOverLimitElements() {
        LinkedList<Integer> list = new LinkedList<>();

        int i;
        for (i = 0; i < limit; i++) {
            queue.add(i);
        }
        for (;i < limit*2; i++) {
            queue.add(i);
            list.add(i);
        }

        assertEquals(limit, queue.size());
        assertTrue(queue.isFull());
        assertEquals(list.peekLast(), queue.getYoungest());
        assertEquals(list.peekFirst(), queue.getOldest());

        for (i = 0; i < limit; i++) {
            assertEquals(list.pollLast(), queue.pop());
        }

        assertEquals(null, queue.pop());
    }

}
