package com.stardust.concurrent;

import androidx.annotation.NonNull;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Stardust on 2017/12/30.
 */

public class ConcurrentArrayList<T> {

    private final Class<T> mTClass;
    private final AtomicInteger mSize = new AtomicInteger();
    private final Object mArrayResizeLock = mSize;
    private volatile T[] mArray;

    public ConcurrentArrayList(Class<T> tClass) {
        mTClass = tClass;
        mArray = newArray(10);
    }

    public void add(T element) {
        int index = mSize.getAndIncrement();
        ensureCapacity(index);
        mArray[index] = element;
    }

    @SuppressWarnings("unchecked")
    public T get(int i) {
        int size = mSize.get();
        if (i >= size) {
            throw new IndexOutOfBoundsException("i = " + i + ", size = " + size);
        }
        return mArray[i];
    }

    public int size() {
        return mSize.get();
    }

    @NonNull
    public Object getArrayResizeLock() {
        return mArrayResizeLock;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private T[] newArray(int size) {
        return (T[]) Array.newInstance(mTClass, size);
    }

    private void ensureCapacity(int index) {
        if (index < mArray.length)
            return;
        synchronized (mArrayResizeLock) {
            if (index < mArray.length)
                return;
            T[] newArray = newArray(mArray.length * 2);
            System.arraycopy(mArray, 0, newArray, 0, mArray.length);
            mArray = newArray;
        }
    }

}
