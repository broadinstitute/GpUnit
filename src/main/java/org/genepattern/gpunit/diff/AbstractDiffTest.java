package org.genepattern.gpunit.diff;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstraction of the 'diff' function for validating a GenePattern job result file.
 * A gpunit test can optionally declare a list of expected result files. Each of those expected result files
 * can optionally declare a diff function.
 * 
 * Create a concrete subclass of this abstract class in order to customize the diff algorithm. This is required
 * for example if you want your diff test to account for arbitrary ordering of elements in the result file,
 * or if you want to provide a tolerance when comparing numerical values.
 * 
 * The gpunit system expects that the diff command will throw appropriate junit assertions in order to signal 
 * a failed test.
 * 
 * @author pcarr
 */
public abstract class AbstractDiffTest {
    protected File inputDir;
    protected List<String> args;
    protected File expected;
    protected File actual;
    //may be helpful for sending an error message
    protected String jobId;

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
    public void setInputDir(File f) {
        this.inputDir = f;
    }
    public void setArgs(final List<String> in) {
        //set by copy
        this.args = new ArrayList<String>();
        for(String arg : in) {
            this.args.add(arg);
        }
    }
    public void setExpected(File expected) {
        this.expected = expected;
    }
    public void setActual(File actual) {
        this.actual = actual;
    }
    
    /**
     * This method is the junit test case. Use the junit Assert class to indicate a failed test case.
     */
    abstract public void diff();
}
