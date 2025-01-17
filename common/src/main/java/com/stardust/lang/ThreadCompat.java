package com.stardust.lang;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Created by Stardust on 2017/4/30.
 */

public class ThreadCompat extends Thread {

    // FIXME: 2017/12/29 是否需要用synchronizedMap?这里虽然线程不安全，但竞争很小
    private static final Set<Thread> interruptedThreads = Collections.newSetFromMap(new WeakHashMap<Thread, Boolean>());

    public ThreadCompat() {
    }

    public ThreadCompat(Runnable target) {
        super(target);
    }

    public ThreadCompat(ThreadGroup group, Runnable target) {
        super(group, target);
    }

    public ThreadCompat(@NonNull String name) {
        super(name);
    }

    public ThreadCompat(ThreadGroup group, @NonNull String name) {
        super(group, name);
    }

    public ThreadCompat(Runnable target, @NonNull String name) {
        super(target, name);
    }

    public ThreadCompat(ThreadGroup group, Runnable target, @NonNull String name) {
        super(group, target, name);
    }

    public ThreadCompat(ThreadGroup group, Runnable target, @NonNull String name, long stackSize) {
        super(group, target, name, stackSize);
    }

    public static boolean interrupted() {
        boolean interrupted = Thread.currentThread().isInterrupted();
        interruptedThreads.remove(Thread.currentThread());
        Thread.interrupted();
        return interrupted;
    }

    @Override
    public boolean isInterrupted() {
        return super.isInterrupted() || interruptedThreads.contains(this);
    }

    @Override
    public void interrupt() {
        super.interrupt();
        interruptedThreads.add(this);
    }


}
