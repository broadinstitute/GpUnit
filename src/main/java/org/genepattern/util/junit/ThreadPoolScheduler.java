package org.genepattern.util.junit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.runners.model.RunnerScheduler;

public class ThreadPoolScheduler implements RunnerScheduler {
    private ExecutorService executor;
    private int numThreads = 16;
    private int shutdownTimeout = 5*60; //seconds
    
    public ThreadPoolScheduler() {
        //init from sys properties
        String numThreadsProp = System.getProperty("junit.parallel.threads");
        if (numThreadsProp != null) {
            numThreads = Integer.parseInt(numThreadsProp);
        }
        String timeoutProp = System.getProperty("junit.parallel.shutdown.timeout");
        if (timeoutProp != null) {
            shutdownTimeout = Integer.parseInt(timeoutProp);
        }
        executor = Executors.newFixedThreadPool(numThreads);
    }

    public void finished() {
        executor.shutdown();
        try {
            //end all tests after the shutdownTime period (default 5 minutes), regardless
            executor.awaitTermination(shutdownTimeout, TimeUnit.SECONDS);
        } 
        catch (InterruptedException exc) {
            throw new RuntimeException(exc);
        }
    }

    public void schedule(Runnable childStatement) {
        executor.submit(childStatement);
    }
}
