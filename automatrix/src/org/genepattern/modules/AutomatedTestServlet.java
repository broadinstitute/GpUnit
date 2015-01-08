package org.genepattern.modules;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.apache.log4j.Logger;
import org.genepattern.gpunit.GpAssertions;
import org.genepattern.gpunit.TestFileObj;
import org.genepattern.webservice.*;
import org.genepattern.util.LSID;
import org.genepattern.util.GPConstants;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import org.genepattern.gpunit.ModuleTestObject;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: nazaire
 * Date: Dec 4, 2012
 * Time: 3:58:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class AutomatedTestServlet extends HttpServlet
{
    private static String OS = System.getProperty("os.name").toLowerCase();
    public static Logger log = Logger.getLogger(AutomatedTestServlet.class);
    public static String TEST_RESULTS_DIR = "test_results";

    public static final String SAVE_TESTS = "/saveTests";
    public static final String GET_GROUP_NAMES = "/getGroupNames";
    public static final String LOAD_PARAM_SETS = "/loadParamSets";
    public static final String GET_TEST_RESULTS = "/getTestResults";
    public static final String RUN_TESTS = "/runTests";
    public static final String ADD_PSET_GROUP_LOCATION = "/addPSetGroupLocation";
    public static final String REMOVE_PSET_GROUP_LOCATION = "/removePSetGroupLocation";
    public static final String GET_MODULE_VERSIONS = "/getModuleVersions";

    private static final String PARAM_SETS_DIR = "param_sets";


    public void doGet(HttpServletRequest request, HttpServletResponse response)
    {
		String action = request.getPathInfo();

       if (SAVE_TESTS.equals(action)) {
		    saveTests(request, response);
		}
        else if (GET_GROUP_NAMES.equals(action)) {
		    getParamSetGroups(request, response);
		}
        else if (LOAD_PARAM_SETS.equals(action)) {
		    loadParamSets(request, response);
		}
        else if (RUN_TESTS.equals(action)) {
		    runTests(request, response);
		}
        else if (GET_TEST_RESULTS.equals(action)) {
		    getTestResults(request, response);
		}
        else if(ADD_PSET_GROUP_LOCATION.equals(action))
        {
            addPSetGroupLocation(request, response);
        }
        else if(REMOVE_PSET_GROUP_LOCATION.equals(action))
        {
            removePSetGroupLocation(request, response);
        }
        else if(GET_MODULE_VERSIONS.equals(action))
        {
            loadModuleVersions(request, response);
        }
        else
        {
            sendError(response, "Routing error for " + action);
		}
    }

    private String createNewDirectoryName()
    {
        String result = null;

        try
        {
            //get a random directory name based on temp file
            File file = File.createTempFile("temp", Long.toString(System.nanoTime()));
            result = file.getName();
            if(!file.delete())
            {
                result = null;
            }
        }
        catch(IOException io)
        {
            log.error(io.getMessage());
        }

        return result;
    }

    private JsonObject findParamSetGroups(String dirName) throws Exception
    {
        JsonObject paramSetsGroups = new JsonObject();

        File dir = new File(dirName);

        if(!dir.exists())
        {
            throw new Exception("The directory " + dirName + " does not exist");
        }
        File[] files = dir.listFiles();
        for(int i=0;i<files.length;i++)
        {
            if(files[i].isDirectory())
            {
                File[] paramSetFiles = files[i].listFiles();
                for(int p=0;p<paramSetFiles.length;p++)
                {
                    if(paramSetFiles[p].getName().endsWith("json"))
                    {
                        //JsonStreamParser parser = new JsonStreamParser(new FileReader(paramSetFiles[p]));
                        //JsonObject groupInfoObject = parser.next().getAsJsonObject();
                        //paramSetsGroups.add(files[i].getName(), groupInfoObject);

                        Gson gson = new Gson();
                        JsonObject groupInfoObject = gson.fromJson(new FileReader(paramSetFiles[p]), JsonObject.class);
                        paramSetsGroups.add(files[i].getName(), groupInfoObject);

                    }
                }
            }
        }

        return paramSetsGroups;
    }

    public void getParamSetGroups(HttpServletRequest request, HttpServletResponse response)
    {
        BufferedReader reader = null;

        try
        {
            JsonObject responseObject = new JsonObject();

            File paramSetsDir = new File(getServletContext().getRealPath("."), PARAM_SETS_DIR);
            log.error("The default param sets dir is " + paramSetsDir);

            JsonObject pSetLocations = new JsonObject();

            if(paramSetsDir.exists())
            {
                JsonObject standardLocParamSetsGroups = findParamSetGroups(paramSetsDir.getCanonicalPath());
                if(standardLocParamSetsGroups.entrySet().size() > 0)
                {
                    pSetLocations.add(paramSetsDir.getCanonicalPath(), standardLocParamSetsGroups);
                }
            }

            File pSetGroupLocationsFile = new File(getServletContext().getRealPath(PARAM_SETS_DIR), "pSetGrouplocations.txt");
            if(pSetGroupLocationsFile.exists())
            {
                reader = new BufferedReader(new FileReader(pSetGroupLocationsFile));

                String directory = null;
                while((directory = reader.readLine()) != null)
                {
                    log.error("Found parameter set search location: " + directory);
                    if(!directory.equals(""))
                    {
                        pSetLocations.add(directory, findParamSetGroups(directory));
                    }
                }

                reader.close();
            }

            if(pSetLocations.entrySet().size() == 0)
            {
                sendError(response, "No parameter sets found!");
            }

            responseObject.add("param_sets_by_loc", pSetLocations);
            this.write(response, responseObject);
        }
        catch(Exception e)
        {
            log.error(e);
            sendError(response, e.getMessage());
        }
        finally
        {

            if(reader != null)
            {
                try{reader.close();}catch(IOException e){}
            }

        }

    }

    private JsonObject getAssertionsObj(GpAssertions gpAssertions)
    {
        JsonObject assertionsObj = new JsonObject();

        assertionsObj.addProperty("jobStatus", gpAssertions.getJobStatus());
        assertionsObj.addProperty("exitCode", gpAssertions.getExitCode());

        if(gpAssertions.getNumFiles() != -1)
        {
            assertionsObj.addProperty("numFiles", gpAssertions.getNumFiles());
        }


        if(gpAssertions.getFiles() != null && gpAssertions.getFiles().size() > 0)
        {
            JsonArray files = new JsonArray();

            for (Map.Entry<String, TestFileObj> entry : gpAssertions.getFiles().entrySet())
            {
                JsonObject paramsObj = new JsonObject();

                JsonObject diffObj = new JsonObject();
                if(entry.getValue() != null)
                {
                    diffObj.addProperty("diff", entry.getValue().getDiff());

                    Gson gson = new Gson();
                    if (entry.getValue().getDiffCmd() != null) {
                        JsonElement diffCmd = gson.toJsonTree(entry.getValue().getDiffCmd());

                        diffObj.add("diffCmd", diffCmd);
                    }

                    paramsObj.add(entry.getKey(), diffObj);
                }
                else
                {
                    //if it is null then assume this is !!null
                    // which just checks for existence of the fill
                    paramsObj.add(entry.getKey(), new JsonPrimitive("!!null"));
                }

                files.add(paramsObj);
            }

            assertionsObj.add("files", files);
        }

        return assertionsObj;
    }

    public void loadParamSets(HttpServletRequest request, HttpServletResponse response)
    {
        String paramSetGroupName = request.getParameter("paramSetGroupName");
        String paramSetGroupLocation =  request.getParameter("paramSetGroupLoc");

        if (paramSetGroupName == null) {
            sendError(response, "No parameter set group name received");
            return;
        }
        try
        {
            JsonObject responseObject = new JsonObject();
            JsonObject paramSetGroup = new JsonObject();
            paramSetGroup.addProperty("group_info", paramSetGroupName);

            log.info("paramSetGroupName: " + paramSetGroupName);
            File paramSetDir = new File(paramSetGroupLocation, paramSetGroupName);
            if(!paramSetDir.exists())
            {
                throw new Exception("An error occurred while loading " +
                        "parameter set group name " + paramSetGroupName);
            }
            
            File[] files = paramSetDir.listFiles();
            Arrays.sort(files);
            JsonArray jsonParamsArray = new JsonArray();
            for(int i=0;i<files.length;i++)
            {
                File file = files[i];

                if(file.isDirectory() || file.isHidden())
                {
                    continue;
                }

                if(file.getName().endsWith("yaml") ||
                        file.getName().endsWith("yml"))
                {
                    Yaml yaml = new Yaml();

                    ModuleTestObject modObj = yaml.loadAs(new FileReader(file), ModuleTestObject.class);

                    JsonObject paramSetGroupObj = new JsonObject();

                    String pSetId = file.getName();
                    pSetId = pSetId.replace(".yml", "");
                    pSetId = pSetId.replace(".yaml", "");
                    paramSetGroupObj.addProperty("id", pSetId);

                    paramSetGroupObj.addProperty("module", modObj.getModule());
                    paramSetGroupObj.addProperty("name", modObj.getName());

                    JsonArray paramsArray = new JsonArray();
                    Map<String, Object> paramMap = modObj.getParams();
                    for (Map.Entry<String, Object> entry : paramMap.entrySet())
                    {
                        JsonObject paramsObj = new JsonObject();

                        Gson gson = new Gson();
                        JsonElement jsonValue = new JsonPrimitive("");
                        if(entry.getValue() != null)
                        {
                            jsonValue = gson.toJsonTree(entry.getValue());
                        }
                        paramsObj.add("value", jsonValue);
                        paramsObj.addProperty("name", entry.getKey());
                        paramsArray.add(paramsObj);

                    }
                    paramSetGroupObj.add("params", paramsArray);

                    JsonObject assertionsObj = getAssertionsObj(modObj.getAssertions());

                    paramSetGroupObj.add("assertions", assertionsObj);

                    jsonParamsArray.add(paramSetGroupObj);
                }
                if(file.getName().endsWith("json"))
                {
                    JsonObject groupInfoObject = new Gson().fromJson(new FileReader(file), JsonObject.class);
                    paramSetGroup.add("group_info", groupInfoObject);
                }
            }

            paramSetGroup.add("param_sets", jsonParamsArray);
            responseObject.add("param_sets_info", paramSetGroup);
            log.error("parameter set group is: " + paramSetGroup);
            this.write(response, responseObject);
        }
        catch(Exception e)
        {
            log.error(e);
            sendError(response, e.getMessage());
        }
    }

    private void extractFromJson(JsonElement jsonElement, PrintWriter writer, String indent)
    {
        if (jsonElement.isJsonObject())
        {
            JsonObject jsonObj =  jsonElement.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObj.entrySet()) {
                writer.print(indent + entry.getKey() + ":");
                if (entry.getValue().isJsonObject() || entry.getValue().isJsonArray()) {
                    writer.println();
                    indent += " ";
                    extractFromJson(entry.getValue(), writer, indent);
                } else {
                    writer.println(" " + entry.getValue().getAsString());
                }
            }
        }

        else if (jsonElement.isJsonArray())
        {
            JsonArray jsonArray =  jsonElement.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                if (element.isJsonObject() || element.isJsonObject()) {
                    indent += " ";
                    extractFromJson(element, writer, indent);
                    writer.println("");
                } else {
                    writer.println(" " + element.getAsString());
                }
            }
        }
    }

    public void saveTests(HttpServletRequest request, HttpServletResponse response)
    {
        String parameterSets = request.getParameter("paramSets");

        if (parameterSets == null) {
            sendError(response, "No parameter sets received");
            return;
        }

        String overwriteTests = request.getParameter("overwrite");
        boolean overwrite = true;

        if(overwriteTests != null && !overwriteTests.equals("true"))
        {
            overwrite = false;
        }

        File paramSetDir = null;
        File tempParamSetDir = null;
        try
        {
            JsonParser jsonParser = new JsonParser();
            JsonObject parameterGroupJSON = jsonParser.parse(parameterSets).getAsJsonObject();
            JsonObject parameterGroupInfoJSON = parameterGroupJSON.get("group_info").getAsJsonObject();

            String directoryName = parameterGroupInfoJSON.get("name").getAsString();

            if( directoryName == null)
            {
                directoryName = createNewDirectoryName();
            }

            if(directoryName == null)
            {
                throw new Exception("An error occurred while saving parameter sets");
            }

            log.error("real path" + this.getServletContext().getRealPath(PARAM_SETS_DIR));

            File paramSetMainDir = new File(getServletContext().getRealPath(PARAM_SETS_DIR));
            boolean result = true;
            if(!paramSetMainDir.exists())
            {
                result = paramSetMainDir.mkdir();
            }

            if(!result)
            {
                throw new Exception("An error occurred while saving parameter sets");
            }

            tempParamSetDir = new File (paramSetMainDir, "tempParamSet1231");
            paramSetDir = new File(paramSetMainDir, directoryName);

            if(paramSetDir.exists())
            {
                if(!overwrite)
                {
                    //parameter set group already exists so throw error
                    throw new  AlreadyExistsException("Parameter set group " + directoryName + " already exists");
                }
            }

            result = tempParamSetDir.mkdir();

            if(!result)
            {
                throw new Exception("An error occurred while creating parameter sets group directory");
            }

            JsonObject groupInfoObjectJSON = parameterGroupJSON.get("group_info").getAsJsonObject();
            File groupInfoFile = new File(tempParamSetDir, "group_info.json");
            //contains global data needed by test automation tool
            PrintWriter groupInfoWriter = null;
            try
            {
                groupInfoWriter = new PrintWriter(groupInfoFile);
                Gson gson = new Gson();
                groupInfoWriter.write(gson.toJson(groupInfoObjectJSON));
            }
            catch(Exception e)
            {
                log.error(e);        
            }
            finally
            {
                if(groupInfoWriter != null)
                {
                    groupInfoWriter.close();
                }
            }


            JsonArray parameterSetsJSON = parameterGroupJSON.get("param_sets").getAsJsonArray();
            for(int p=0;p<parameterSetsJSON.size();p++)
            {
                JsonObject parameterSet = parameterSetsJSON.get(p).getAsJsonObject();
                log.error("json pset: " + parameterSet);
                
                String pSetKey = parameterSet.get("id").getAsString();

                String pSetFileName = pSetKey;
                if(pSetKey != null && !pSetKey.endsWith("_test"))
                {
                    pSetFileName += "_test.yml";
                }
                else
                {
                    pSetFileName += ".yml";
                }
                
                File paramSetFile = new File(tempParamSetDir, pSetFileName);

                PrintWriter writer = null;

                try
                {
                    writer = new PrintWriter(paramSetFile);
                    writer.println("#lsid=" + groupInfoObjectJSON.get("lsid").getAsString());
                    writer.println("#");
                    writer.println("name: " + parameterSet.get("name").getAsString());
                    //check if there is a note or description specified
                    if(parameterSet.has("description") && parameterSet.get("description").getAsString() != null && parameterSet.get("description").getAsString().length() > 1)
                    {
                        writer.println("description: " + parameterSet.get("description").getAsString());
                    }

                    writer.println("module: " + groupInfoObjectJSON.get("module").getAsString());

                    JsonArray parameters = parameterSet.get("params").getAsJsonArray();
                    writer.println("params:");                    
                    for(int i=0;i<parameters.size();i++)
                    {
                        JsonObject parameter = parameters.get(i).getAsJsonObject();
                        Object value = parameter.get("value");

                        writer.println("       " +
                                parameter.get("name").getAsString() + ": " + value);
                    }

                    JsonObject assertions = parameterSet.getAsJsonObject("assertions");
                    writer.println("assertions:");
                    String indent  =" ";
                    extractFromJson(assertions, writer, indent);
                }
                finally
                {
                    writer.close();
                }
            }

            //if we get here then parameter sets was successfully saved
            //so renamed to temp location to the specified name of the parameter set group
            if(paramSetDir.exists())
            {
                //delete the existing parameter sets group directory
                Util.delete(paramSetDir);
            }
            
            tempParamSetDir.renameTo(paramSetDir);
        }
        catch (Exception e) {
            log.error("Error: " + e.getMessage(), e);

            if(!(e instanceof AlreadyExistsException) && !overwrite &&  paramSetDir != null && paramSetDir.exists())
            {
                if(!Util.delete(paramSetDir))
                {
                    log.error("An error occurred when removing parameter set" +
                            "directory: " + paramSetDir.getAbsolutePath());
                }

                if(tempParamSetDir != null)
                {
                    Util.delete(tempParamSetDir);
                }
            }
            sendError(response, "Unable to save parameter sets: " + e.getMessage());
        }

        //delete the temp dir
        Util.delete(tempParamSetDir);

        JsonObject message = new JsonObject();
        message.addProperty("MESSAGE", "Parameter sets saved");
        this.write(response, message);
    }

    public void runTests(HttpServletRequest request, HttpServletResponse response)
    {
        String server = request.getParameter("server");
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String paramSetGroupName = request.getParameter("paramSetGroupName");
        String paramSetGroupLocation =  request.getParameter("paramSetGroupLoc");
        String gpUnitTimeout = request.getParameter("gpUnitTimeout");
        String gpUnitNumThreads = request.getParameter("gpUnitNumThreads");
        String gpUnitClient = request.getParameter("gpUnitClient");
        String gpUnitServerDir = request.getParameter("gpUnitServerDir");
        String lsid = request.getParameter("lsid");

        if (server == null)
        {
            sendError(response, "No GP server received");
            return;
        }

        if (username == null)
        {
            sendError(response, "No username received");
            return;
        }

        if (paramSetGroupName == null) {
            log.error("No parameter set group name received");
	        sendError(response, "No parameter set group name received");
	        return;
	    }

        log.error("running tests: " + paramSetGroupName);
        File paramSetDir = null;

        if(paramSetGroupLocation == null)
        {
            paramSetDir = new File(getServletContext().getRealPath(PARAM_SETS_DIR), paramSetGroupName);
        }
        else
        {
            paramSetDir = new File(paramSetGroupLocation, paramSetGroupName);
            if(!paramSetDir.exists())
            {
                log.error("Could not find parameter the set group directory: " + paramSetGroupLocation);
                sendError(response, "Could not find parameter the set group directory: " + paramSetGroupLocation);
                return;
            }
        }

        server = server.replace("http://", "");
        server = server.replace("/gp", "");

        String port = "";
        int colonIndex = server.indexOf(":");
        if(colonIndex != -1)
        {
            port = server.substring(colonIndex);
            server = server.substring(0, colonIndex);
        }

        SimpleDateFormat format = new SimpleDateFormat("MMM_d_yyyy_hh_mm_a");
        format.format(new Date(System.currentTimeMillis()));

        String timeStamp = format.format(new Date(System.currentTimeMillis()));
        File testResultsDir =  new File(paramSetDir, TEST_RESULTS_DIR);
        testResultsDir.mkdir();
        File outputDir = new File(testResultsDir, timeStamp);
        outputDir.mkdir();

        log.info("output dir is: " + outputDir);

        String reportDir = (new File(outputDir, "reports")).getAbsolutePath();
        //replace \ with / in file path so that it does not need to be escaped when running on Windows
        reportDir = reportDir.replace("\\", "/");
        log.error("report dir: " + reportDir);


        //Create properties file
        PrintWriter writer = null;
        File gpUnitPropertiesfile = null;
        File yamlFilesDir = paramSetDir;
        try
        {
            //Check if the specific version of the module was selected
            //in this case we need to rewrite the yaml file to set the module version
            if(lsid != null && lsid.length() > 1)
            {
                yamlFilesDir = new File(outputDir, "yaml");
                if(!yamlFilesDir.mkdir())
                {
                    sendError(response, "An error occurred while creating directory: " +
                            yamlFilesDir.getAbsolutePath());
                }
                File[] yamlFiles = paramSetDir.listFiles();
                for(int f=0;f<yamlFiles.length;f++)
                {
                    yamlFilesDir.deleteOnExit();
                    File newYamlFile = new File(yamlFilesDir, yamlFiles[f].getName());
                    PrintWriter yamlFileWriter = null;
                    BufferedReader yamlFileReader = null;
                    try
                    {
                        yamlFileWriter = new PrintWriter(newYamlFile);
                        yamlFileReader = new BufferedReader(new FileReader(yamlFiles[f]));
                        String line = null;
                        while((line = yamlFileReader.readLine()) != null)
                        {
                            if(line.startsWith("module:"))
                            {
                                line = "module: " + lsid;
                            }

                            yamlFileWriter.println(line);
                        }
                    }
                    catch(IOException io)
                    {
                        io.printStackTrace();
                    }
                    finally
                    {
                        if(yamlFileReader != null)
                        {
                            yamlFileReader.close();
                        }

                        if(yamlFileWriter != null)
                        {
                            yamlFileWriter.close();
                        }
                    }
                }
            }

            gpUnitPropertiesfile = new File(outputDir, "gpunit.properties");

            writer = new PrintWriter(gpUnitPropertiesfile);
            writer.println("gp.user=" + username);
            writer.println("gp.password=" + password);
            writer.println("gp.host=" + server);
            writer.println("gp.port=" + port);
            writer.println("gpunit.delete.jobs=false"); //do not delete jobs

            //replace \ with / in file path so that it does not need to be escaped when running on Windows
            writer.println("gpunit.testfolder=" + yamlFilesDir.getAbsolutePath().replace("\\", "/"));

            if(gpUnitServerDir != null)
            {
                //remove any extra spaces
                gpUnitServerDir = gpUnitServerDir.trim();

                writer.println("gpunit.server.dir=" + gpUnitServerDir);
            }

            log.error("gpUnit client: " + gpUnitClient);
            if(gpUnitClient != null)
            {
                //remove any extra spaces
                gpUnitClient = gpUnitClient.trim();

                writer.println("gpunit.client=" + gpUnitClient);
            }

            if(gpUnitTimeout != null)
            {
                //remove any extra spaces
                gpUnitTimeout = gpUnitTimeout.trim();

                //check if this is an integer
                try
                {
                    Integer.parseInt(gpUnitTimeout);
                    writer.println("gpunit.shutdowntimeout=" + gpUnitTimeout);
                }
                catch(Exception io)
                {
                    //log error and continue
                    log.error("Shutdown timeout is not an integer: " + gpUnitTimeout);
                }
            }

            if(gpUnitNumThreads != null)
            {
                //remove any extra spaces
                gpUnitNumThreads = gpUnitNumThreads.trim();

                //check if this is an integer
                try
                {
                    Integer.parseInt(gpUnitTimeout);
                    writer.println("gpunit.numthreads=" + gpUnitNumThreads);
                }
                catch(Exception io)
                {
                    //log error and continue
                    log.error("Num threads is not an integer: " + gpUnitNumThreads);
                }
            }

            //replace \ with / in file path so that it does not need to be escaped when running on Windows
            writer.println("gpunit.outputdir=" + (new File(outputDir, "jobResults")).getAbsolutePath().replace("\\", "/"));
            writer.println("gpunit.save.downloads=true");
            
            //String reportDir = getServletContext().getRealPath(PARAM_SETS_DIR) +  "/" + paramSetGroupName + "/" + gpUnitPropertiesfile.getName() + "_reports/";
            writer.println("report.dir="+ reportDir);
        }
        catch(Exception e)
        {
            log.error(e.getMessage());
            sendError(response, "Error: " + e.getMessage());
            return;
        }
        finally
        {
            if(writer != null)
            {
                writer.close();
            }
        }

        //now Run tests
        PrintWriter testResultWriter = null;
        PrintWriter testResultErrorWriter = null;
        try
        {
            ServletContext context = this.getServletContext();
            String gpUnitBuildFilePath = context.getRealPath("/WEB-INF/gpunit/build.xml");

            log.info("gpunit build file url: " + gpUnitBuildFilePath);

            ProcessBuilder pb = null;

            if (OS.indexOf("win") != -1)
            {
                log.info("Windows style command line: " + "cmd.exe /C ant -f " + gpUnitBuildFilePath + " gpunit " +
                        "-Dgpunit.properties="+ gpUnitPropertiesfile.getAbsolutePath());
                pb = new ProcessBuilder("cmd.exe", "/C", "ant -f " + gpUnitBuildFilePath + " gpunit " +
                        "-Dgpunit.properties="+ gpUnitPropertiesfile.getAbsolutePath());
            }
            else
            {
                log.info("Unix style command line: " + "ant -f " + gpUnitBuildFilePath + " gpunit " +
                        "-Dgpunit.properties="+ gpUnitPropertiesfile.getAbsolutePath());

                System.out.println("Unix style command line: " + "ant -f " + gpUnitBuildFilePath + " gpunit " +
                        "-Dgpunit.properties=" + gpUnitPropertiesfile.getAbsolutePath());

                log.info("PATH env: " + System.getenv("PATH"));

                System.out.println("PATH env: " + System.getenv("PATH"));
                pb = new ProcessBuilder("ant", "-f", gpUnitBuildFilePath, "gpunit",
                    "-Dgpunit.properties="+ gpUnitPropertiesfile.getAbsolutePath());
            }
            pb.directory(paramSetDir);
            Process process = pb.start();

            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            testResultWriter = new PrintWriter(
                    new File(outputDir, "output.log"));
            String line;
            while ((line = br.readLine()) != null) {
                testResultWriter.println(line);
            }

            is = process.getErrorStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);

            testResultErrorWriter = new PrintWriter(
                    new File(outputDir, "error.log"));
            while ((line = br.readLine()) != null) {
                testResultErrorWriter.println(line);
            }
        }
        catch(IOException e)
        {
            sendError(response, "An error occurred while running tests: " + e.getMessage());
        }
        finally
        {
            if(testResultWriter != null)
            {
                testResultWriter.close();
            }

            if(testResultErrorWriter != null)
            {
                testResultErrorWriter.close();
            }
        }
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("MESSAGE", "Finished running tests");

        this.write(response, responseObject);
    }

    public void loadModuleVersions(HttpServletRequest request, HttpServletResponse response)
    {
        String server = request.getParameter("server");
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String lsid = request.getParameter("lsid");

        if (server == null) {
            sendError(response, "No GP server received");
            return;
        }

        if (username == null) {
            sendError(response, "No username received");
            return;
        }

        if (lsid == null) {
            sendError(response, "No lsid received");
            return;
        }

        try
        {
            AdminProxy adminProxy = new AdminProxy(server, username, password);

            TaskInfo taskInfo = adminProxy.getTask(lsid);
            if(taskInfo == null)
            {
                throw new Exception ("An error occurred while retreiving task info");
            }

            JsonObject responseObject = new JsonObject();

            List versions = (List)adminProxy.getLSIDToVersionsMap().get(new LSID(lsid).toStringNoVersion());

            Gson gson = new Gson();
            JsonElement versionsArray = gson.toJsonTree(versions, new TypeToken<List<String>>() {}.getType());

            responseObject.add("lsidVersions", versionsArray);

            this.write(response, responseObject);
        }
        catch(Exception e)
        {
            log.trace(e);

            String message = "";
            if(e.getMessage() != null)
            {
                message = e.getMessage();
            }
            sendError(response, "An error occurred while loading the module with lsid " + lsid + " : " + message);
        }
    }

    public void getTestResults(HttpServletRequest request, HttpServletResponse response)
    {
        try
        {
            log.info("getting test results");
            JsonObject testResultsByParamSet = new JsonObject();

            //Look in param sets directory for finished tests
            File paramSetDir = new File(getServletContext().getRealPath(PARAM_SETS_DIR));
            FileFilter testDirFilter = new FileFilter()
            {
                public boolean accept(File name)
                {
                    if(name.isDirectory())
                    {
                        return true;
                    }

                    return false;
                }
            };

            File[] allParamSetDirs = paramSetDir.listFiles(testDirFilter);
            if(allParamSetDirs == null || allParamSetDirs.length < 1)
            {
                throw new Exception("No parameter sets found: " + allParamSetDirs);
            }

            log.info("found test results: " + paramSetDir.getAbsolutePath());

            for(int t=0; t <allParamSetDirs.length; t++)
            {
                File[] testResultsDir = allParamSetDirs[t].listFiles(
                new FileFilter()
                {
                    public boolean accept(File name)
                    {
                        if(name.getName().equals("test_results"))
                        {
                            return true;
                        }
                        return false;
                    }
                });

                if(testResultsDir == null || testResultsDir.length < 1)
                {
                   continue;
                }

                JsonArray testResultNames = new JsonArray();

                File[] testResultDir = testResultsDir[0].listFiles();
                for(int r =0;r<testResultDir.length;r++)
                {
                    log.info("test result dir: " + testResultDir[r]);
                    if(testResultDir[r].isDirectory())
                    {
                        JsonObject testResultDetails = new JsonObject();

                        try {
                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(Long.parseLong(testResultDir[r].getName()));
                            DateFormat df = new SimpleDateFormat("EEE, MMM_d_yyyy_hh_mm_a");
                            testResultDetails.addProperty("name", df.format(cal.getTime()));
                        }catch(NumberFormatException ne)
                        {
                            log.error(ne);
                            //otherwise this is new format for file name
                            //so no need to format
                            testResultDetails.addProperty("name", testResultDir[r].getName());
                        }
                        //check if there is a report html file
                        String reportPath = "/reports";
                        File reportMainDir = new File(testResultDir[r].getAbsolutePath(), reportPath);
                        File[] contents = reportMainDir.listFiles();
                        if(contents != null)
                        {
                            for(File file: contents)
                            {
                                if(file.isDirectory() && !file.getName().equals("current"))
                                {
                                    reportPath += "/" + file.getName();
                                }
                            }
                        }
                        reportPath += "/html/index.html";
                        File reportHtml = new File(testResultDir[r].getAbsolutePath(), reportPath);
                        log.info("report html: " + reportHtml.getAbsolutePath());
                        if(reportHtml.exists())
                        {
                            String reportLink = PARAM_SETS_DIR + "/" + allParamSetDirs[t].getName() + "/"
                                    + testResultsDir[0].getName() + "/" + testResultDir[r].getName() + reportPath;
                            testResultDetails.addProperty("reportLink", reportLink);
                        }

                        testResultNames.add(testResultDetails);
                    }
                }

                testResultsByParamSet.add(allParamSetDirs[t].getName(), testResultNames);
            }

            JsonObject responseObject = new JsonObject();

            responseObject.add("test_results", testResultsByParamSet);
            this.write(response, responseObject);
        }
        catch(Exception e)
        {
            log.error(e);
            sendError(response, e.getMessage());
        }
    }

    public void addPSetGroupLocation(HttpServletRequest request, HttpServletResponse response)
    {
        String location = request.getParameter("location");
        if(location == null || location.equals(""))
        {
            sendError(response, "Empty location specified");
            return;
        }

        PrintWriter writer = null;
        try
        {
            File paramSetsDir = new File(getServletContext().getRealPath(PARAM_SETS_DIR));

            //if the PARAM_SETS_DIR does not exist yet then create it first
            if(!paramSetsDir.exists())
            {
                boolean success = paramSetsDir.mkdir();

                if(!success)
                {
                    sendError(response, "Unable to create directory at: " + paramSetsDir.getAbsolutePath());
                    return;
                }
            }

            File pSetGroupLocationsFile = new File(paramSetsDir, "pSetGrouplocations.txt");

            //check that this new location is valid
            File locationFile = new File(location);
            if(!locationFile.exists())
            {
                sendError(response, "The specified path " + location + " does not exist.");
                return;
            }
            else
            {
                writer = new PrintWriter(new FileWriter(pSetGroupLocationsFile, true));
                writer.println(location);
            }
            JsonObject message = new JsonObject();
            message.addProperty("MESSAGE", "Added parameter set group location: " + location);
            this.write(response, message);
        }
        catch(Exception e)
        {
            log.error(e);
            sendError(response, e.getMessage());
        }
        finally
        {
            if(writer != null)
            {
                writer.close();
            }

        }
    }

    public void removePSetGroupLocation(HttpServletRequest request, HttpServletResponse response)
    {
        String location = request.getParameter("location");
        if(location == null || location.equals(""))
        {
            sendError(response, "Empty location specified");
            return;
        }

        PrintWriter writer = null;
        BufferedReader reader = null;
        try
        {
            File pSetGroupLocationsFile = new File(getServletContext().getRealPath(PARAM_SETS_DIR), "pSetGrouplocations.txt");

            reader = new BufferedReader(new FileReader(pSetGroupLocationsFile));

            File tempFile = File.createTempFile("temp", ".txt");
            writer = new PrintWriter(new FileWriter(tempFile, true));
            String line = null;
            while((line = reader.readLine()) != null)
            {
                if(!line.equals(location))
                {
                    writer.println(location);
                }
            }

            reader.close();

            //rename temp file
            pSetGroupLocationsFile.delete();
            tempFile.renameTo(pSetGroupLocationsFile);

            JsonObject message = new JsonObject();
            message.addProperty("MESSAGE", "Removed parameter set group location: " + location);
            this.write(response, message);
        }
        catch(Exception e)
        {
            log.error(e);
            sendError(response, e.getMessage());
        }
        finally
        {
            if(writer != null)
            {
                writer.close();
            }

            if(reader != null)
            {
                try{reader.close();}catch(IOException e){}
            }

        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
    {
        doGet(request, response);
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response)
    {
        doGet(request, response);
    }

    private void write(HttpServletResponse response, Object content)
    {
        this.write(response, content.toString());
    }

    private void write(HttpServletResponse response, String content)
    {
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.println(content);
            writer.flush();
        }
        catch (IOException e) {
            log.error("Error writing to the response in AutomatedTestServlet: " + content, e);
        }
        finally {
            if (writer != null) writer.close();
        }
    }

    public void sendError(HttpServletResponse response, String message)
    {
	    JsonObject error = new JsonObject();
	    error.addProperty("ERROR", "ERROR: " + message);
	    this.write(response, error);

        log.error(message);
    }

    private ArrayList getModuleVersions(TaskInfo[] tasks, String taskLSID) throws Exception
    {
        String taskNoLSIDVersion = new LSID(taskLSID).toStringNoVersion();
        log.error("task lsid no version: " + taskNoLSIDVersion);

        ArrayList moduleVersions = new ArrayList();        

        for(int i=0;i<tasks.length;i++)
        {
            TaskInfoAttributes tia = tasks[i].giveTaskInfoAttributes();
            String lsidString = tia.get(GPConstants.LSID);
            LSID lsid = new LSID(lsidString);
            String lsidNoVersion = lsid.toStringNoVersion();
            if(taskNoLSIDVersion.equals(lsidNoVersion))
            {
                moduleVersions.add(lsidString);
                log.error("lsid version: " + lsidString);
            }
        }

        return moduleVersions;
    } 
}
