package org.genepattern.gpunit.yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.yaml.snakeyaml.Yaml;

public class ModuleTestParserYaml {
    
    static public ModuleTestObject parse(File testFile) throws GpUnitException, FileNotFoundException {
        InputStream is = null;
        try {
            is = new FileInputStream(testFile);
            ModuleTestObject test = ModuleTestParserYaml.parse(is);
            return test;
        }
        catch (FileNotFoundException e) {
            throw e;
        }
    }

    static public ModuleTestObject parse(InputStream is) throws GpUnitException {
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
