package com.diosaraiva.plantumlgui.util;

// Single factory for the app's worker threads; they are daemons so no pending task can block shutdown.
public final class Threads {

    private Threads() { }

    public static Thread newDaemon(String name, Runnable task) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }
}
