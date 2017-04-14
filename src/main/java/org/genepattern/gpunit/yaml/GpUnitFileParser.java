package org.genepattern.gpunit.yaml;

import java.io.File;
import java.io.IOException;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;

/**
 * Helper class for parsing gpunit files.
 * 
 * @author pcarr
 *
 */
public class GpUnitFileParser {
    /**
     * Fail the test if the testFile is null or does not exist.
     * @param testFile
     * @return
     * @throws GpUnitException
     */
    protected static File checkTestFile(final File testFile) throws GpUnitException {
        if (testFile==null) {
            throw new GpUnitException("Missing required arg: testFile==null");
        }
        if (!testFile.exists()) {
            throw new GpUnitException("File not found, testFile='"+testFile+"'");
        }
        if (!testFile.canRead()) {
            throw new GpUnitException("Permissions error, cannot read testFile='"+testFile+"'");
        }
        return testFile;
    }
    
    protected static File initTestDir(final File testFile) throws GpUnitException {
        if (testFile.isAbsolute()) {
            return testFile.getParentFile();
        }
        else {
            try {
                return testFile.getCanonicalFile().getParentFile();
            } 
            catch (IOException e) {
                throw new GpUnitException(
                    "Error getting test directory path, testFile='"+testFile+"': "+e.getLocalizedMessage(), e);
            }
        }
    }

    /**
     * Create a new ModuleTestObject instance by parsing the given file. The input file can be in one of two formats:
     *     (1) a gp_execution_log.txt file from a completed gp job, or
     *     (2) a yaml formatted file which declares the test case
     * 
     * @param testFile
     * @return
     * @throws Exception
     */
    public static final ModuleTestObject initTestCase(final File testFile) throws GpUnitException {
        // fail early on null input or if the file doesn't exist
        checkTestFile(testFile);
        if ("gp_execution_log.txt".equals(testFile.getName().toLowerCase())) {
            return ExecutionLogParser.parse(testFile);
        }
        ModuleTestObject testCase = ModuleTestParserYaml.parse(testFile);
        //optionally set the test name based on the fromFile path
        testCase.setNameFromFile(testFile);
        return testCase;
    }
    
}
