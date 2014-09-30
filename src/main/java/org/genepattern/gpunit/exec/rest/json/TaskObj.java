package org.genepattern.gpunit.exec.rest.json;

import org.genepattern.gpunit.GpUnitException;

import com.google.gson.JsonObject;

/**
 * Wrapper class for a task object as returned by the 
 * '/rest/v1/tasks/{lsid}' endpoint. 
 * @author pcarr
 *
 */
public class TaskObj {
    private final String lsid;
    
    private TaskObj(final Builder in) {
        this.lsid=in.lsid;
    }
    
    public String getLsid() {
        return lsid;
    }
    
    public static class Builder {
        private String lsid;
        
        public Builder lsid(final String lsid) {
            this.lsid=lsid;
            return this;
        }
        
        public Builder fromJsonObject(final JsonObject jsonObject) throws GpUnitException {
            this.lsid=jsonObject.get("lsid").getAsString();
            return this;
        }
        
        public TaskObj build() {
            return new TaskObj(this);
        }
    }

}
