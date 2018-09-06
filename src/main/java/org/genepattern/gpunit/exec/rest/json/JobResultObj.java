package org.genepattern.gpunit.exec.rest.json;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Wrapper class for a job result, as returned by the REST API call to the 
 * 'rest/v1/jobs/{jobId}' endpoint.
 * @author pcarr
 *
 */
public class JobResultObj {
    public static class OutputFile {
        final String name;
        final String href;
        protected OutputFile(final String name, final String href) {
            this.name=name;
            this.href=href;
        }
        
        public String getName() {
            return name;
        }
        public String getHref() {
            return href;
        } 
    }
    
    private final JsonObject jsonObject;
    private final String jobId;
    private final boolean hasError;
    private final boolean isFinished;
    private final List<OutputFile> outputFiles;
    
    private JobResultObj(Builder in) {
        this.jsonObject=in.jsonObject;
        this.jobId=in.jobId;
        this.hasError=in.hasError;
        this.isFinished=in.isFinished;
        if (in.outputFiles==null) {
            this.outputFiles=Collections.emptyList();
        }
        else {
            this.outputFiles=Collections.unmodifiableList(in.outputFiles);
        }
    }
    
    public JsonObject getJsonObject() {
        return jsonObject;
    }
    
    public String getJobId() {
        return jobId;
    }

    public boolean hasError() {
        return hasError;
    }
    
    public boolean isFinished() {
        return isFinished;
    }
    
    public List<OutputFile> getOutputFiles() {
        return outputFiles;
    }
    
    public void saveJobJsonToDir(final File dir) {
        boolean mkdirs_success=dir.mkdirs();
        final File toFile=new File(dir, jobId+".json");
        boolean success=saveJobJsonToFile(toFile);
        if (!success) {
            System.err.println("    jsonFile.mkdirs: "+mkdirs_success);
        }
    }

    public boolean saveJobJsonToFile(final File jsonFile) {
        PrintWriter logWriter = null;
        try {
            logWriter = new PrintWriter(new FileWriter(jsonFile));
            logWriter.print(jsonObject.toString());
            logWriter.close();
            return true;
        }
        catch (Throwable t) {
            System.err.println("Error writing jsonFile for jobId="+jobId);
            System.err.println("    "+jsonFile);
            t.printStackTrace();
            return false;
        }
        finally {
            logWriter.close();
        }
    }
    
    /**
     * Builder pattern for building JobResultObj instances
     * @author pcarr
     *
     */
    public static class Builder {
        private JsonObject jsonObject=null;
        private String jobId=null;
        private boolean hasError=false;
        private boolean isFinished=false;
        private List<OutputFile> outputFiles=null;
        
        public Builder jobId(final String jobId) {
            this.jobId=jobId;
            return this;
        }

        public Builder hasError(final boolean hasError) {
            this.hasError=hasError;
            return this;
        }
        
        public Builder isFinished(final boolean isFinished) {
            this.isFinished=isFinished;
            return this;
        }
        
        public Builder addOutputFile(final OutputFile outputFile) {
            if (outputFiles==null) {
                outputFiles=new ArrayList<OutputFile>();
            }
            outputFiles.add(outputFile);
            return this;
        }
        
        public JobResultObj build() {
            return new JobResultObj(this);
        }

        // GSON specific code
        public Builder gsonObject(JsonObject jsonObject) {
            // save the json output 
            this.jsonObject=jsonObject;

            // init jobId
            jobId(jsonObject.get("jobId").getAsString());

            //init hasError and isFinished
            final JsonObject status=jsonObject.get("status").getAsJsonObject();
            final boolean hasError=status.get("hasError").getAsBoolean();
            final boolean isFinished=status.get("isFinished").getAsBoolean();
            hasError(hasError);
            isFinished(isFinished);

            // initialize the list of output files
            JsonArray outputFilesJsonArray=jsonObject.get("outputFiles").getAsJsonArray();
            int numFiles=outputFilesJsonArray.size();
            for(int i=0; i<numFiles; ++i) {
                final JsonObject outputFileJsonObj=outputFilesJsonArray.get(i).getAsJsonObject();
                final JsonObject linkJsonObject=outputFileJsonObj.get("link").getAsJsonObject();
                final String name=linkJsonObject.get("name").getAsString();
                final String href=linkJsonObject.get("href").getAsString();
                OutputFile outputFile = new OutputFile(name, href);
                addOutputFile(outputFile);
            }

            return this;
        }
        
    }
}
