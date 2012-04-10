package org.genepattern.gpunit.yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.yaml.snakeyaml.Yaml;

/**
 * Parse an input file into a test case object.
 * 
 * @author pcarr
 */
public class ModuleTestParserYaml {
    static public ModuleTestObject parse(File testFile) throws GpUnitException, FileNotFoundException {
        ModuleTestObject test = null;
        InputStream is = null;
        try {
            is = new FileInputStream(testFile);
            test = parse(is);
            test = initInputdir(testFile, test);
            return test;
        }
        catch (FileNotFoundException e) {
            throw e;
        }
    }
    
    private static ModuleTestObject initInputdir(final File testFile, final ModuleTestObject test) {
        if (test == null) {
            return test;
        }
        if (test.getInputdir() == null) {
            //by default, the inputdir is the parent dir of the testFile
            test.setInputdir( testFile.getParentFile().getAbsoluteFile() );
            return test;
        }

        //else, convert relative path to absolute path
        if (!test.getInputdir().isAbsolute()) {
            //relative paths are relative to the testFile's parent directory
            File nd = new File(testFile.getAbsoluteFile(), test.getInputdir().getPath()).getAbsoluteFile();
            test.setInputdir(nd);
        }
        return test;
    }

    static private ModuleTestObject parse(InputStream is) throws GpUnitException {
        try {
            Yaml yaml = new Yaml();
            ModuleTestObject obj = yaml.loadAs(is, ModuleTestObject.class);
            return obj;
        }
        catch (Throwable t) {
            throw new GpUnitException("Error parsing test file", t);
        }
    }
}
