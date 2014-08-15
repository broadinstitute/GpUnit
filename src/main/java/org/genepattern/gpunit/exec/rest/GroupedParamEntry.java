package org.genepattern.gpunit.exec.rest;

/**
 * Extension of the ParamEntry class to include the optional 'groupId' in the JSON representation.
 * 
 * @author pcarr
 */
public class GroupedParamEntry extends ParamEntry {
    private final String groupId;
    
    public GroupedParamEntry(final String name, final String groupId) {
        super(name);
        this.groupId=groupId;
    }
    
    public String getGroupId() {
        return groupId;
    }
}