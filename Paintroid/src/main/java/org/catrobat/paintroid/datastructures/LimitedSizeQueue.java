package org.catrobat.paintroid.datastructures;


import java.util.ArrayList;

public class LimitedSizeQueue<K> extends ArrayList<K> {

    private int maxSize;

    public LimitedSizeQueue(int size){
        this.maxSize = size;
    }

    public boolean add(K k){
        boolean r = super.add(k);
        if (size() > maxSize){
            removeRange(0, size() - maxSize - 1);
        }
        return r;
    }

    public K getYongest() {
        return get(size() - 1);
    }

    public K getOldest() {
        return get(0);
    }

    public K pop() {
        if (size() > 0) {
            return remove(size() -1);
        } else {
            return null;
        }
    }
}
