package org.genepattern.localexec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.genepattern.webservice.Parameter;

public class LocalJob {
    private String commandLineFromManifest;
    private String[] commandLineArgs;
    private Parameter[] inputParams;
    
    private void initFromManifest(File manifest) throws FileNotFoundException, IOException {
        Properties props = new Properties();
        FileReader fr = null;
        try {
            fr = new FileReader(manifest);
            props.load( fr );
        }
        finally {
            fr.close();
        } 
        commandLineFromManifest = props.getProperty("commandLine");
    }
    
    private void initFromInputParams() {
    }
}
