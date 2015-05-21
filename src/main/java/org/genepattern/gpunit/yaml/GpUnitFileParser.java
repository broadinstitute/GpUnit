package org.genepattern.gpunit.yaml;

import java.io.File;

import org.genepattern.gpunit.ModuleTestObject;

/**
 * Helper class for parsing gpunit files.
 * 
 * @author pcarr
 *
 */
public class GpUnitFileParser {
    /**
     * Create a new ModuleTestObject instance by parsing the given file. The input file can be in one of two formats:
     *     (1) a gp_execution_log.txt file from a completed gp job, or
     *     (2) a yaml formatted file which declares the test case
     * 
     * @param fromFile
     * @return
     * @throws Exception
     */
    public static final ModuleTestObject initTestCase(final File fromFile) throws Exception {
        if ("gp_execution_log.txt".equals(fromFile.getName().toLowerCase())) {
            return ExecutionLogParser.parse(fromFile);
        }
        ModuleTestObject testCase = ModuleTestParserYaml.parse(fromFile);
        //optionally set the test name based on the fromFile path
        testCase.setNameFromFile(fromFile);
        return testCase;
    }
    
}
