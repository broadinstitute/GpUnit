package org.genepattern.util.junit;

import org.genepattern.gpunit.test.BatchProperties;
import org.junit.runners.Parameterized;


/**
 * Run junit tests in parallel.
 * 
 * @author pcarr
 */
public class Parallelized 
    extends Parameterized 
{
    
    public Parallelized(Class<?> klass) throws Throwable {
        super(klass);
        int numThreads=BatchProperties.getIntegerProperty(BatchProperties.PROP_NUM_THREADS, 32);
        int shutdownTimeout=BatchProperties.initShutdownTimeout();
        ThreadPoolScheduler scheduler=new ThreadPoolScheduler(numThreads, shutdownTimeout);
        setScheduler(scheduler);
    }

}


