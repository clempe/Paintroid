package org.catrobat.paintroid.datastructures;


import java.util.LinkedList;

public class LimitedSizeQueue<E>  {

    private int maxSize;
    private LinkedList<E> linkedList = new LinkedList<>();

    public LimitedSizeQueue(int size)
    {
        if (size <= 0) {
            throw new IllegalArgumentException("The size must be greater than 0");
        }

        this.maxSize = size;
    }

    public E add(E e){
        linkedList.add(e);
        if (size() > maxSize){
            return linkedList.pollFirst();
        }
        return null;
    }

    public E getYoungest() {
        return linkedList.getLast();
    }

    public E getOldest() {
        return linkedList.getFirst();
    }

    public E pop() {
        return linkedList.pollLast();
    }

    public int size() {
        return linkedList.size();
    }

    public boolean isFull(){
        return size() == maxSize;
    }

    public boolean isEmpty(){
        return size() == 0;
    }

    public int maxSize() {return maxSize;}
}
