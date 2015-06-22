package org.genepattern.gpunit.yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.io.InputStream;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;

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

    static public ModuleTestObject parse(InputStream is) throws GpUnitException {
        try {
            Yaml yaml = new Yaml();
            ModuleTestObject obj = yaml.loadAs(is, ModuleTestObject.class);
            return obj;
        }
        catch (ParserException e) {
            throw new GpUnitException("Error parsing test yaml file: "+e.toString());
        }
        catch (Throwable t) {
            // Try to get the originating exception and extract a useful error message. This is useful if
            // the exception was originally thrown by user-code during YAML parsing (i.e., a reference
            // to an undefined property during property substitution). In order to get a detailed
            // error message to the user we need to dive down to the original InvocationTargetException.
            // This is somewhat dependent on snakeyaml's exception propagation implementation.
            for (Throwable cause = t; cause != null; cause = cause.getCause()) {
                if (cause instanceof InvocationTargetException) {
                    InvocationTargetException target = (InvocationTargetException) cause;
                    Throwable ie = target.getTargetException();
                    if (ie != null) {
                        String s = ie.getLocalizedMessage();
                        throw new GpUnitException(s);
                    }
                }
            }
            throw new GpUnitException("Error parsing test yaml file: ", t);
        }
    }
}
