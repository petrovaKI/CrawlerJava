package org.parser;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadRunner {
    private final int numThreads;
    private final ExecutorService executor;

    public ThreadRunner(int numThreads) {
        this.numThreads = numThreads;
        this.executor = Executors.newFixedThreadPool(numThreads);
    }

    public void runTasks(Callable<Void> task) {
        for (int i = 0; i < numThreads; i++) {
            executor.submit(task);
        }
        executor.shutdown();
    }
}

