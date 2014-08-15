package org.genepattern.gpunit.exec.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JSON representation of an input value for the REST API. Each entry in the list of values
 * must be a valid URL.
 * 
 * Examples,
 * 
 * A single value
 * <pre>
   { name: "input.filename",
     values: [ "ftp://gpftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.cls" ]
   }
 * </pre>
 * 
 * A list of values
 * <pre>
   { name: "input.filename",
     values: [ "fileA.txt", "fileB.txt" ]  <--- each entry must be a valid URL   
   }
 * </pre>
 * 
 * A grouped list of values is represented by a List of ParamEntry, each with the same name.
 * <pre>
   { name: "input.filename",
     groupId: "Condition A",
     values: [ "fileA.txt", "fileB.txt" ]
   },
   { name: "input.filename",
     groupId: "Condition B",
     values: [ "fileC.txt", "fileD.txt" ]
   }
 * </pre>
 * @author pcarr
 *
 */
public class ParamEntry {
    /** The name of the input parameter. */
    private final String name;
    /** A list of zero or more values for the input parameter. */
    private List<String> values=new ArrayList<String>();

    /**
     * Initialize with a parameter name.
     * @param name
     */
    public ParamEntry(final String name) {
        this.name=name;
    }
    
    public String getName() {
        return name;
    }

    public List<String> getValues() {
        return Collections.unmodifiableList(values);
    }
    
    public void addValue(final String value) {
        this.values.add(value);
    }

}