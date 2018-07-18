package org.genepattern.gpunit.exec.rest.json;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.genepattern.gpunit.GpUnitException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Wrapper class for a task object as returned by the 
 * '/rest/v1/tasks/{lsid}' endpoint. 
 * @author pcarr
 *
 */
public class TaskObj {
    private final String lsid;
    private final Map<String,JsonObject> params;
    
    private TaskObj(final Builder in) {
        this.lsid=in.lsid;
        this.params=in.params;
    }
    
    public boolean hasParam(final String pname) {
        return params.containsKey(pname);
    }
    
    public boolean isFileParam(final String pname) {
        return 
            params.containsKey(pname) 
            &&
            "java.io.File".equals(
                params.get(pname).getAsJsonObject("attributes").get("type").getAsString()
            );
    }
    
    public String getLsid() {
        return lsid;
    }
    
    public static class Builder {
        private String lsid;
        private Map<String, JsonObject> params;
        
        public Builder lsid(final String lsid) {
            this.lsid=lsid;
            return this;
        }
        
        public Builder fromJsonObject(final JsonObject jsonObject) throws GpUnitException {
            this.lsid=jsonObject.get("lsid").getAsString(); 
            this.params=new HashMap<String, JsonObject>(); 
            JsonArray params=jsonObject.getAsJsonArray("params");
            for(int i=0; i<params.size(); ++i) {
                for(final Entry<String, JsonElement> entry : params.get(i).getAsJsonObject().entrySet()) {
                    this.params.put(entry.getKey(), entry.getValue().getAsJsonObject());
                }
            }
            return this;
        }
        
        public TaskObj build() {
            return new TaskObj(this);
        }
    }

}
