package org.genepattern.util.junit;

import java.util.Properties;

import org.genepattern.gpunit.BatchProperties;
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
        
        final Properties sysProps=System.getProperties();
        int numThreads=BatchProperties.getIntegerProperty(sysProps, BatchProperties.PROP_NUM_THREADS, 32);
        int shutdownTimeout=BatchProperties.initShutdownTimeout(sysProps);
        ThreadPoolScheduler scheduler=new ThreadPoolScheduler(numThreads, shutdownTimeout);
        setScheduler(scheduler);
    }

}


