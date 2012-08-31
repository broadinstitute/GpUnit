package org.genepattern.util.junit;

import org.junit.runners.Parameterized;


/**
 * Run junit tests in parallel,
 * set 'junit.parallel.threads' system property to define the number of jobs to run in parallel.
 * 
 * @author pcarr
 */
public class Parallelized 
    extends Parameterized 
{
    
    public Parallelized(Class klass) throws Throwable{
        super(klass);
        setScheduler(new ThreadPoolScheduler());
    }
    

}


