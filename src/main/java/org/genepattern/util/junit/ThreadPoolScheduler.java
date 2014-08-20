package org.genepattern.util.junit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.runners.model.RunnerScheduler;

public class ThreadPoolScheduler implements RunnerScheduler {
    private ExecutorService executor;
    private final int shutdownTimeout;
    
    public ThreadPoolScheduler() {
        // initialize with default values
        this(32, 300);
    }
    
    public ThreadPoolScheduler(final int numThreads, final int shutdownTimeout) {
        this.shutdownTimeout=shutdownTimeout; //BatchProperties.getIntegerProperty(BatchProperties.PROP_SHUTDOWN_TIMEOUT, 300);
        executor = Executors.newFixedThreadPool(numThreads);
    }

    public void finished() {
        executor.shutdown();
        try {
            //end all tests after the shutdownTime period (default 5 minutes), regardless
            // returns true if this executor terminated and false if the timeout elapsed before termination
            boolean terminated=executor.awaitTermination(shutdownTimeout, TimeUnit.SECONDS);
            if (!terminated) {
                //kill the running jobs
                executor.shutdownNow();
            }
        } 
        catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exc);
        }
    }

    public void schedule(Runnable childStatement) {
        executor.submit(childStatement);
    }
}
