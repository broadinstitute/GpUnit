package org.genepattern.gpunit.diff;

import org.broadinstitute.matrix.Dataset;
import org.junit.Assert;

/**
 * Compare expected and actual result files based on number of rows and columns,
 * rather than comparing each numerical value.
 * 
 * Implementation note: This implementation is based on the original GP regression tests,
 * developed by Jared Nedzel, using the GPclient, and special-code from the gp-modules.jar library for
 * reading and comparing GP data files, such as gct, res, et cetera.
 * 
 * The utility methods in the gp-modules.jar library are not thread-safe, which caused the tests to 
 * fail when ported to gp-unit, with parallel execution of tests. 
 * 
 * I modified the library, only to handle gct files. Which is good enough to meet our existing test-cases.
 * 
 * However, we should improve upon this with a more general matrix diff utility, which is in the works.
 * 
 * @author pcarr
 *
 */
public class NumRowsColsDiff extends AbstractDiffTest {
    //expected args, --numRows=<numRows> --numCols=<numCols> <expected> <actual>
    
    private int expectedNumRows=-1;
    private int expectedNumCols=-1;
    
    public void setExpectedNumRows(final int rows) {
        this.expectedNumRows = rows;
    }
    
    public void setExpectedNumCols(final int cols) {
        this.expectedNumCols = cols;
    }
    
    private void initFromArgs() {
        if (jobId == null) {
            jobId="";
        }
        else {
            jobId = "job #"+jobId+", ";
        }
        if (args != null) {
            for(String arg : args) {
                handleArg(arg);
            }
        }
    }
    private void handleArg(String arg) {
        if  (arg==null) {
            Assert.fail(jobId+"Unexpected null arg initializing custom diff class="+NumRowsColsDiff.class.getName());
        }
        if (arg.startsWith("--numCols=")) {
            expectedNumCols = initIntFromArg(arg);
        }
        else if (arg.startsWith("--numRows=")) {
            expectedNumRows=initIntFromArg(arg);
        }
        else {
            Assert.fail(jobId+"Unexpected cmdLine arg initializing custom diff class="+NumRowsColsDiff.class.getName()+", arg="+arg);
        }
    }
    
    private int initIntFromArg(String arg) {
        String[] split = arg.split("=");
        if (split.length==2) {
            String val = split[1];
            try {
                int intVal = Integer.parseInt(val);
                return intVal;
            }
            catch (Throwable t) {
                Assert.fail(jobId+"Invalid cmdLine arg initializing custom diff class="+NumRowsColsDiff.class.getName()+", arg="+arg);
            }
        }
        Assert.fail(jobId+"Invalid cmdLine arg initializing custom diff class="+NumRowsColsDiff.class.getName()+", arg="+arg);
        return -1;
    }

    @Override
    public void diff() {
        initFromArgs();
        if (expectedNumCols >= 0 || expectedNumRows >= 0) {
            Dataset dataset = null;
            try {
                dataset = IOUtilThreadSafe.Singleton.instance().readDataset(actual.getAbsolutePath());
            }
            catch (Throwable t) {
                Assert.fail(jobId+"Error reading dataset for file='"+actual.getAbsolutePath()+"': "+t.getLocalizedMessage());
            }
            Assert.assertNotNull(dataset);
            if (expectedNumRows >= 0) {
                Assert.assertEquals(jobId+"'"+actual.getName()+"'[numRows]", expectedNumRows, dataset.getRowCount());
            }
            if (expectedNumCols >= 0) {
                Assert.assertEquals(jobId+"'"+actual.getName()+"'[numCols]", expectedNumCols, dataset.getColumnCount());
            }
        }
    }

}
