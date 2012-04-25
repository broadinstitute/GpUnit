package org.genepattern.localexec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.genepattern.webservice.Parameter;

public class LocalJob {
    private String commandLineFromManifest;
    private String[] commandLineArgs;
    private Parameter[] inputParams;
    
    private void initFromManifest(File manifest) throws FileNotFoundException, IOException {
        Properties props = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(manifest);
            props.load( in );
        }
        finally {
            in.close();
        } 
        commandLineFromManifest = props.getProperty("commandLine");
    }
    
    private void initFromInputParams() {
    }
}
