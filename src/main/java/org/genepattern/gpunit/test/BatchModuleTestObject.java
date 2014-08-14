package org.genepattern.gpunit.test;

import java.io.File;

import org.genepattern.gpunit.ModuleTestObject;

/**
 * For running a parameterized test, you need a list of these.
 * The BatchModuleUtil class creates lists of these objects.
 * 
 * @author pcarr
 */
public class BatchModuleTestObject {
    private File testFile;
    private ModuleTestObject testCase;
    private Throwable initException = null;
    
    public void setTestFile(final File f) {
        this.testFile = f;
    }
    public File getTestFile() {
        return testFile;
    }
    
    public void setTestCase(final ModuleTestObject t) {
        this.testCase = t;
    }
    
    public ModuleTestObject getTestCase() {
        return testCase;
    }
    
    public void setInitException(Throwable t) {
        this.initException = t;
    }
    
    public Throwable getInitException() {
        return initException;
    }
    
    public String getTestName() {    
        if (testCase != null && testCase.getName() != null) {
            return testCase.getName();
        }
        else if (testFile != null) {
            return testFile.getName();
        }
        else {
            return "";
        }
    }
    
    public String toString() {
        return getTestName();
    }
    
}
