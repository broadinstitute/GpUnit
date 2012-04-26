package org.genepattern.util.junit;


/**
 * Run junit tests in parallel,
 * set 'junit.parallel.threads' system property to define the number of jobs to run in parallel.
 * 
 * @author pcarr
 */
public class LabelledParallelized extends LabelledParameterized {
    
    public LabelledParallelized(Class klass) throws Throwable{
        super(klass);
        setScheduler(new ThreadPoolScheduler());
    }
}


