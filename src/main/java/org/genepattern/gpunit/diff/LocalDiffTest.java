package org.genepattern.gpunit.diff;

import java.io.File;

/**
 * 
 * @author pcarr
 */
public abstract class LocalDiffTest extends AbstractDiffTest {

    protected File inputDir;
    protected File expected;
    protected File actual;

    public void setInputDir(File f) {
        this.inputDir = f;
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
