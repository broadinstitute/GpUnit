package org.genepattern.gpunit;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.genepattern.gpunit.test.BatchModuleUtil;
import org.genepattern.gpunit.yaml.GpUnitFileParser;
import org.junit.Test;

public class TestBatchModuleUtil {
    public static void assertFileExists(final File file) {
        assertTrue("file does not exist: '"+file.getPath()+"'", file.exists());
    }

    @Test(expected=GpUnitException.class)
    public void initTestCase_fileNotFoundError() throws GpUnitException {
        GpUnitFileParser.initTestCase(new File("src/test/data/missing.yml"));
    }

    @Test(expected=GpUnitException.class)
    public void initTestObject_fileNotFoundError() throws Throwable {
        final File testFile = new File("src/test/data/missing.yml");
        BatchModuleTestObject testObj=BatchModuleUtil.initBatchModuleTestObject(testFile);
        assertTrue(testObj.hasInitExceptions());
        throw testObj.getInitException();
    }

    @Test
    public void initDataArray_fileNotFound() {
        // file does not exist
        final File testFile = new File("src/test/data/missing.yml");
        final Collection<Object[]> data=BatchModuleUtil.data(Arrays.asList(testFile));
        assertEquals("BatchModuleUtil.data.size, skip missing files", 0, data.size());
    }

    @Test(expected=GpUnitException.class)
    public void initTestCase_parseError_missing_params() throws GpUnitException {
        // yaml file with 'params:' set to null 
        final File testFile=new File("src/test/data/parseError.yaml");
        assertFileExists(testFile);
        GpUnitFileParser.initTestCase(testFile);
    }
    
    @Test
    public void initTestObject_empty_params() {
        File testFile=new File("src/test/data/empty_params.yml");
        assertFileExists(testFile);
        BatchModuleTestObject testObj=BatchModuleUtil.initBatchModuleTestObject(testFile);
        assertNotNull("expecting non-null testObj", testObj);
        if (testObj.getInitException() != null) {
            fail("Unexpected exception: "+testObj.getInitException().getLocalizedMessage());
        }
    }
}
