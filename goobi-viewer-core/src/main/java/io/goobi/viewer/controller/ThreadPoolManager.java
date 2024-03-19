package io.goobi.viewer.controller;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.poi.ss.formula.functions.T;

public class ThreadPoolManager {

    private final ExecutorService executorService;
    private final Object lock = new Object();

    public ThreadPoolManager(int size) {
        this.executorService = Executors.newFixedThreadPool(size);
    }

    public Future<T> execute(Callable<T> task) {
        synchronized (lock) {
            return this.executorService.submit(task);
        }
    }

    public Future<?> execute(Runnable task) {
        synchronized (lock) {
            return this.executorService.submit(task);
        }
    }

    public void shutdown() {
        synchronized (lock) {
            this.executorService.shutdownNow();
        }
    }
    
    public ExecutorService getExecutorService() {
        synchronized (lock) {
            return executorService;            
        }
    }

}
