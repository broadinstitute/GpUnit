var overwriteTests = true;

var test_editor = {
    lsid: "",
    module: "",
    server: "",
    username: "",
    currentParamSetGroup: "",
    pSetGroupLocs: [] //List of paths to search for parameter set groups to load
};

//parameter object from server
var parametersJson;

//contains configuration settings to create parameter sets
var pset_config =
{
    //numInvalidTest: 0,
    //numValidTests: 0
};

var parameterSets = {};

var mainLayout;
var centerInnerLayout;
var westInnerLayout;


//For those browsers that dont have it so at least they won't crash.
if (!window.console)
{
    window.console = { time:function(){}, timeEnd:function(){}, group:function(){}, groupEnd:function(){}, log:function(){} };
}

function htmlEncode(value)
{
  if(value == undefined || value == null || value == "")
  {
      return value;
  }
    
  return $('<div/>').text(value).html();
}

function htmlDecode(value)
{
    if(value == undefined || value == null || value == "")
    {
        return value;
    }

    return $('<div />').html(value).text();
}

function htmlEscape(str) {
    return String(str)
            .replace(/&/g, '&amp;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
}

function trim(s)
{
	var l=0; var r=s.length -1;
	while(l < s.length && s[l] == ' ')
	{	l++; }
	while(r > l && s[r] == ' ')
	{	r-=1;	}
	return s.substring(l, r+1);
}

function make_base_auth(user, password) {
    var tok = user + ':' + password;
    var hash = btoa(tok);
    return "Basic " + hash;
}

function loadAllTasks()
{
    var ALL_TASKS_REST = "/rest/v1/tasks/all.json";
    var includeHidden = "?includeHidden=true";

    $.ajaxSetup({
        headers: {
            'Authorization': make_base_auth(test_editor.username, $("#password").val())
        }
    });

    return $.ajax({
        url: test_editor.server + ALL_TASKS_REST + includeHidden,
        type: 'GET',
        dataType: 'json',
        crossDomain: true,
        xhrFields: {
            withCredentials: true
        }
    }).fail(function( jqXHR, textStatus, errorThrown )
    {
        var Error = "Error status: " + jqXHR.status;
        if(errorThrown != null && errorThrown != "")
        {
            Error += "\nMessage: " + htmlEncode(errorThrown);
        }

        createErrorMsg("Get Module List", "Error retrieving module list. \n" + Error);

    }).done(function(data, textStatus, jqXHR )
    {
        var modules = data['all_modules'];

        loadAllTasksInfo(modules);
    });
}

function loadAllTasksInfo(modules)
{
    //remove all module rows
    $("#mTable").empty();
    
    for(var i=0; i < modules.length;i++)
    {
        if(modules[i].name !== undefined)
        {
            var mTRow = $("<tr/>");
            var moduleLink = $("<a href='#'>" + modules[i].name + "</a>");
            moduleLink.val(modules[i].lsid);
            moduleLink.click(function(event)
            {
                event.preventDefault();
                test_editor["module"] = $(this).text();

                loadModule($(this).val());
            });
            
            var mTd = $("<td/>");
            mTd.append(moduleLink);
            mTRow.append(mTd);
            $("#mTable").append(mTRow);
        }
    }
}

function loadModuleInfo(taskId, lsidVersions)
{
    //remove header that appears on view parameter set page
    $("#groupSetDiv").remove();

    //remove span containing the name of the selected task
    $("#selectedTask").remove();

    var selectModulePanel = $(
        '<div id="selectModulePanel"> ' +
            '   <span id="topLeft" class="header"></span> ' +
            '   <span id="showDescriptionSpan" class="floatRight"> ' +
            '       <input type="checkbox" id="showDescription"/>   ' +
            '       <label for="showDescription">Show parameter descriptions</label><br/>  ' +
            '   </span> ' +
            '</div>');

    $(".middle-north").empty();
    $(".middle-north").append(selectModulePanel);

    $("#showDescription").click(function()
    {
        var isChecked = $(this).is(':checked');

        if(isChecked)
        {
            $(".pDescription").show();
        }
        else
        {
            $(".pDescription").hide();
        }

    });

    $("#topLeft").append("<span id='selectedTask'>" + test_editor.module + " version </span>");
    if(lsidVersions != undefined && lsidVersions != null)
    {
        var index = test_editor.lsid.lastIndexOf(":");
        if(index == -1)
        {
            createErrorMsg("Module Versions", "An error occurred while loading module versions.\nInvalid lsid: " + $(this).val());
        }

        var lsidNoVersion = test_editor.lsid.substring(0, index);

        if(lsidVersions.length == 1)
        {
            $("#selectedTask").append(lsidVersions[0]);
        }
        else
        {
            var versionSelect = $("<select/>");
            versionSelect.change(function()
            {
                loadModule($(this).val());
            });

            for(var l=0;l<lsidVersions.length;l++)
            {
               versionSelect.append("<option value='" + lsidNoVersion + ":" + lsidVersions[l] + "'>" + lsidVersions[l] +"</option>");
            }

            versionSelect.val(taskId);
            $("#selectedTask").append(versionSelect);
        }
    }

    centerInnerLayout.open("north");
}

function loadModule(taskId)
{
    test_editor["lsid"] = taskId;

    $.ajax({
        type: "POST",
        url: window.location.pathname + "AutomatedTest/getModuleVersions",
        data: { "lsid" : taskId,
            "server" : test_editor.server,
            "username" : test_editor.username,
            "password": $("#password").val()},
        success: function(response) {
            loadModuleInfo(taskId, response["lsidVersions"]);
        },
        error: function (xhr, ajaxOptions, thrownError) {
            createErrorMsg("Get Module Versions", "Error status: " + xhr.status);
            if(thrownError != null && thrownError != "")
            {
                createErrorMsg("Get Module Versions", htmlEncode(thrownError));
            }
        },
        dataType: "json"
    });


    var REST_ALL_TASKS = "/rest/v1/tasks/";

    $.ajax({
        type: "GET",
        url: test_editor.server + REST_ALL_TASKS + taskId,
        xhrFields: {
            withCredentials: true
        },
        success: function(response) {
            var params = response['params'];
            loadParameterInfo(params);
        },
        error: function (xhr, ajaxOptions, thrownError) {
            createErrorMsg("Load Module", "Error status: " + xhr.status);
            if(thrownError != null && thrownError != "")
            {
                createErrorMsg("Load Module", htmlEncode(thrownError));
            }
        },
        dataType: "json"
    });
}

function populateChoiceDiv(parameter, container)
{
    var REST_ALL_TASKS = "/rest/v1/tasks/";

    $.ajax({
        type: "GET",
        url: test_editor.server + REST_ALL_TASKS +  test_editor["module"] + "/" + parameter + "/choiceInfo.json",
        xhrFields: {
            withCredentials: true
        },
        success: function(response) {
            choiceInfo = response;
            var longChars = 1;
            var fileChoice = $("<select class='fileChoice'/>");

            for(var c=0;c<choiceInfo.choices.length;c++)
            {
                fileChoice.append("<option value='"+choiceInfo.choices[c].value+"'>"
                    + choiceInfo.choices[c].label+"</option>");
                if(choiceInfo.choices[c].label.length > longChars)
                {
                    longChars = choiceInfo.choices[c].label.length;
                }
            }

            container.append(fileChoice);

            fileChoice.multiselect({
                multiple: true,
                header: true,
                selectedList: 1
            });
            fileChoice.data("pName", parameter);

            fileChoice.multiselect("refresh");

            fileChoice.change(function()
            {
                var pName = $(this).data("pName");

                var selectedValues = $(this).multiselect("getChecked");

                var validValues = [];
                if(selectedValues.length > 0)
                {
                    for(var s=0;s<selectedValues.length;s++)
                    {
                        validValues.push(selectedValues[s].value);
                    }
                }

                addPropertyToParamConfig(pName, "valid", validValues);
                addPropertyToParamConfig(pName, "type","choice");
            });

        },
        error: function (xhr, ajaxOptions, thrownError) {
            createErrorMsg("Load Module", "Error status: " + xhr.status);
            if(thrownError != null && thrownError != "")
            {
                createErrorMsg("Load Module", htmlEncode(thrownError));
            }
        },
        dataType: "json"
    });
}

function jqEscape(str)
{
    return str.replace(/([;&,\.\+\*\~':"\!\^$%@\[\]\(\)=>\|])/g, '\\$1');
}

function loadParameterInfo(parametersArray)
{
    var parametersChoiceMap = {};

    //clear contents of parameter sets
    parameterSets = {};
    pset_config = {};

    //clear the center div
    $("#paramDiv").remove();
    $(".middle-center").empty();
    $(".middle-south").empty();

    centerInnerLayout.open("north");
    centerInnerLayout.sizePane("north", "47");

    var paramDiv = $("<div id='paramDiv'/>");
    paramDiv.append("<table id='pTable'/>");
    $(".middle-center").append(paramDiv);


    $(".middle-south").append("<button id='createTestButton'>Create Parameter Sets</button>");
    $("#createTestButton").button();
    
    if(parametersArray.length == 0)
    {
        $("#pTable").append("<tr><td> No input parameters found </td></tr>");
        return;
    }

    for(var i=0; i < parametersArray.length;i++)
    {
        var paramsObject = parametersArray[i];
        var paramName = Object.keys(paramsObject);
        if(paramName.length == 1)
        {
            var parameter = paramsObject[paramName];

            if(parameter.attributes != undefined)
            {
                var pTRow = $("<tr/>");
                $("#pTable").append(pTRow);

                var pName= paramName[0];

                pTRow.data("pname", pName);

                //replace . in parameter name with a space
                var pNameWithSpace = pName.replace(/\./g,' ');

                if(parameter.attributes.altName != undefined && parameter.attributes.altName != null
                    && parameter.attributes.altName.length > 0)
                {
                    pNameWithSpace = parameter.attributes.altName.replace(/\./g,' ');
                    addPropertyToParamConfig(pName, "altName", parameter.attributes.altName);
                }

                var optional = true;
                if(parameter.attributes.optional.length <= 0)
                {
                    optional = false;
                    pNameWithSpace += "*";
                }

                addPropertyToParamConfig(pName, "optional", optional);
                pTRow.append("<td>" + pNameWithSpace +"</td>");

                var pTypeSelector = $("<select class='pSelector'/>");
                var multiSelect = true;

                var pType = parameter.attributes.type;
                addPropertyToParamConfig(pName, "default", parameter.attributes.default_value);

                var choiceInfo = parameter.choiceInfo;
                var isChoiceInfo = choiceInfo != undefined && choiceInfo != null && choiceInfo != "";
                if(pType == "java.io.File" || (parameter.attributes.TYPE.toLowerCase() == "file" && parameter.attributes.MODE.toLowerCase() == "in"))
                {
                    pTypeSelector.append("<option value='SERVER_FILE'>Enter server file path</option>");
                    multiSelect = false;

                    isChoiceInfo = parameter.attributes.choiceDir != undefined && parameter.attributes.choiceDir != null
                        && parameter.attributes.choiceDir != "";
                    //check if this is a dynamic file drop down
                    if(isChoiceInfo)
                    {
                        pTypeSelector.prepend("<option value='SELECT_FILE'>Select file from list</option>");

                        var choice = $("<select class='fileChoice'/>");

                        pTypeSelector.parent("td").append(choice);

                        var choiceFileSelectRow = $("<tr id='" + jqEscape(pName)+ "_choice' class='choiceFile'/>");

                        choiceFileSelectRow.append("<td/>");
                        var choiceFileTd = $("<td/>");
                        choiceFileSelectRow.append(choiceFileTd);
                        $("#pTable").append(choiceFileSelectRow);

                        populateChoiceDiv(pName, choiceFileTd);
                    }
                    addPropertyToParamConfig(pName, "type", "file");


                    var serverFileInput = $("<input class='fileInput' type='text'/>");
                    serverFileInput.data("pName", pName);
                    serverFileInput.change(function()
                    {
                        var pName = $(this).data("pName");
                        var fileValue = $(this).val();

                        var fileList = [];
                        fileList.push(fileValue);

                        addPropertyToParamConfig(pName, "valid", fileList);
                        addPropertyToParamConfig(pName, "isFile", true);
                    });

                    var serverFileTd = $("<td/>");
                    serverFileTd.append(serverFileInput);

                    var serverFileSelectRow = $("<tr id='" + jqEscape(pName)+ "_file' class='serverFile'/>");
                    serverFileSelectRow.append("<td/>");

                    serverFileSelectRow.append(serverFileTd);

                    $("#pTable").append(serverFileSelectRow);
                    serverFileSelectRow.hide();
                }
                else
                {
                    var defaultValue = "";
                    defaultValue = parameter.attributes.default_value;

                    var defaultFound = false;
                    var firstChoiceValue = null;
                    var choiceMap = {};
                    //get display value if this is a choice list
                    if(isChoiceInfo)
                    {
                        var result = choiceInfo.choices;

                        for(var j=0;j<result.length;j++)
                        {
                            var displayValue = result[j].label;
                            var value = result[j].value;

                            if(value == parameter.attributes.default_value)
                            {
                                defaultFound = true;
                            }
                            //if no default value is specified for the
                            //then the first choice will be used
                            if(j==0)
                            {
                                firstChoiceValue = value;
                            }

                            choiceMap[value] = displayValue;
                        }

                        parametersChoiceMap[pName] = choiceMap;

                        if(!defaultFound)
                        {
                            parameter.attributes.default_value = firstChoiceValue;
                        }

                        defaultValue = parametersChoiceMap[pName][parameter.attributes.default_value];
                    }

                    var option = $("<option/>");
                    option.append(htmlEncode(defaultValue) + " (default)");
                    option.val(htmlEncode(parameter.attributes.default_value));
                    pTypeSelector.append(option);

                    addPropertyToParamConfig(pName, "default", parameter.attributes.default_value);

                    if(choiceMap != null && Object.keys(choiceMap).length > 0)
                    {
                        var choiceKeys = Object.keys(choiceMap);
                        for(var k =0; k < choiceKeys.length; k++)
                        {
                            var key = choiceKeys[k];

                            //do not add listing of default values
                            if(key == parameter.attributes.default_value)
                            {
                                continue;
                            }
                            var optionItem = $("<option/>");
                            optionItem.append(htmlEncode(choiceMap[key]));
                            optionItem.val(htmlEncode(key));

                            pTypeSelector.append(optionItem);
                        }
                        addPropertyToParamConfig(pName, "type", "choice");
                    }
                    else
                    {
                        addPropertyToParamConfig(pName, "type", "field");

                        multiSelect = false;
                        //add option for use to specify specific values only if this is not a choice list
                        pTypeSelector.append("<option value='custom'>Specify values</option>");
                    }

                    if((choiceMap == null || Object.keys(choiceMap).length == 0) && (pType == "java.lang.Integer" || pType == "java.lang.Float"))
                    {
                        pTypeSelector.append("<option value='integer'>Integer</option>");
                        pTypeSelector.append("<option value='float'>Float</option>");
                    }
                }

                var pTd = $("<td colspan='1'/>");
                pTd.append(pTypeSelector);

                pTypeSelector.multiselect(
                {
                    position: {
                        my: 'left bottom',
                        at: 'left top'
                    },
                    minWidth: 300,
                    multiple: multiSelect,
                    selectedList: 1,
                    header: false
                });

                pTRow.append(pTd);

                if(pType != "java.io.File")
                {
                    var fixedValueTd = $("<td/>");
                    var $fixedValueCheckbox = $("<input type='checkbox'/>");
                    $fixedValueCheckbox.click(function()
                    {
                        var paramName = $(this).parents("tr:first").data("pname");
                        console.log("fixed val name: " + paramName);
                        if($(this).is(":checked"))
                        {
                            addPropertyToParamConfig(paramName, "isFixed", true);
                        }
                        else
                        {
                            addPropertyToParamConfig(paramName, "isFixed", false);
                        }
                    });

                    fixedValueTd.append($fixedValueCheckbox);
                    fixedValueTd.append("Fixed value");

                    pTRow.append(fixedValueTd);
                }


                //add descriptions
                $("#pTable").append("<tr class='pDescription'><td></td><td colspan='2'>" + htmlEncode(parameter.description) +"</td></tr>");
                $(".pDescription").hide();
            }
        }
    }

    var groupInfo = parameterSets["group_info"];
    if(groupInfo == undefined || groupInfo == null)
    {
        groupInfo = {};
        parameterSets["group_info"] = groupInfo;
    }
    groupInfo["choices"] = parametersChoiceMap;

    $(".pSelector").trigger("change");

    centerInnerLayout.open('north');
    centerInnerLayout.open('south');
    $("#paramDiv").show();
}

function loadAllParamSetGroups()
{
    $.ajax({
        type: "POST",
        url: window.location.pathname + "AutomatedTest/getGroupNames",
        data: { "server" : test_editor.server,
                "username" : test_editor.username,
                "password": $("#password").val(),
                "searchLocations": JSON.stringify(test_editor.pSetGroupLocs)
        },
        success: function(response) {
            var message = response["MESSAGE"];
            var error = response["ERROR"];

            if (error !== undefined && error !== null)
            {
                createErrorMsg("Get Parameter Set Names", htmlEncode(error));
                return;
            }

            if (message !== undefined && message !== null) {
                createInfoMsg("Get Parameter Set Names", message);
                return;
            }

            loadAllParamSetsInfo(response["param_sets_by_loc"]);
        },
        error: function (xhr, ajaxOptions, thrownError) {
            createErrorMsg("Get Parameter Set Names", "Error status: " + xhr.status);
            if(thrownError != null && thrownError != "")
            {
                createErrorMsg("Get Parameter Set Names", htmlEncode(thrownError));
            }
        },
        dataType: "json"
    });
}

function addPSetGroupLocation(location)
{
    $.ajax({
        type: "POST",
        url: window.location.pathname + "AutomatedTest/addPSetGroupLocation",
        data: { "location": location },
        success: function(response) {
            var message = response["MESSAGE"];
            var error = response["ERROR"];

            if (error !== undefined && error !== null)
            {
                createErrorMsg("Add Parameter Set Location", htmlEncode(error));
                return;
            }

            if (message !== undefined && message !== null) {
                createInfoMsg("Add Parameter Set Location",message);
                return;
            }

            //now reload the list of parameter set groups
            loadAllParamSetGroups();
        },
        error: function (xhr, ajaxOptions, thrownError) {
            createErrorMsg("Add Parameter Set Location", "Error status: " + xhr.status);
            if(thrownError != null && thrownError != "")
            {
                createErrorMsg("Add Parameter Set Location", htmlEncode(thrownError));
            }
        },
        dataType: "json"
    });
}

function removePSetGroupLocation(location)
{
    $.ajax({
        type: "POST",
        url: window.location.pathname + "AutomatedTest/removePSetGroupLocation",
        data: { "location": location },
        success: function(response) {
            var message = response["MESSAGE"];
            var error = response["ERROR"];

            if (error !== undefined && error !== null)
            {
                createErrorMsg("Remove Parameter Set", htmlEncode(error));
                return;
            }

            if (message !== undefined && message !== null) {
                createInfoMsg("Remove Parameter Set", message);
                return;
            }

            //now reload the list of parameter set groups
            loadAllParamSetGroups();
        },
        error: function (xhr, ajaxOptions, thrownError) {
            createErrorMsg("Remove Parameter Set", "Error status: " + xhr.status);
            if(thrownError != null && thrownError != "")
            {
                createErrorMsg("Remove Parameter Set", htmlEncode(thrownError));
            }
        },
        dataType: "json"
    });
}

function loadAllTestResults()
{
    $.ajax({
        type: "POST",
        url: window.location.pathname + "AutomatedTest/getTestResults",
        success: function(response) {
            var message = response["MESSAGE"];
            var error = response["ERROR"];

            if (error !== undefined && error !== null)
            {
                createErrorMsg("Get Test Results", htmlEncode(error));
                return;
            }
            if (message !== undefined && message !== null) {
                createInfoMsg("Get Test Results",message);
                return;
            }

            loadAllTestsResultsInfo(response["test_results"]);
        },
        error: function (xhr, ajaxOptions, thrownError) {
            createErrorMsg("Get Test Results", "Error status: " + xhr.status);
            if(thrownError != null && thrownError != "")
            {
                createErrorMsg("Get Test Results", htmlEncode(thrownError));
            }
        },
        dataType: "json"
    });
}

function loadAllTestsResultsInfo(testResultsByParamSet)
{
    $(".testResultList").remove();

    var listingDiv = $("<div class='testResultList'/>");
    var paramSetNames = Object.keys(testResultsByParamSet);
    for(var p=0;p<paramSetNames.length;p++)
    {
        var listingDiv = $("<div class='testResultList'/>");
        listingDiv.append("<h3>" + paramSetNames[p] + "</h3>");
        var listingDetails = $("<ul/>");
        listingDiv.append(listingDetails);

        var testResults = testResultsByParamSet[paramSetNames[p]];
        for(var t=0;t<testResults.length;t++) {
            if (testResults[t].reportLink != undefined && testResults[t].reportLink != null)
            {
                var rLink = $("<a href='#'>" + testResults[t].name + "</a>");
                $("<li/>").append(rLink).appendTo(listingDetails);

                rLink.data("link", testResults[t].reportLink);
                rLink.data("pset", paramSetNames[p]);
                rLink.click(function (event) {
                    event.preventDefault();

                    $(".middle-center").empty();
                    $(".middle-north").empty();
                    centerInnerLayout.open("north");
                    centerInnerLayout.sizePane("north", 47);
                    $(".middle-north").append("<div class='header'>" + $(this).data("pset") + ": " + $(this).text() + "</div>");
                    $(".middle-center").append('<iframe src="' + $(this).data("link") + '" width="97%" height="97%"></iframe>');
                });
            }
            else
            {
                $("<li/>").append(testResults[t].name + "(no report)").appendTo(listingDetails);
            }
        }
        $("#viewTestResults").append(listingDiv);
        listingDiv.accordion({
            collapsible: true
        });
    }
}

function configureAndRunTests()
{
    //show reused login dialog to enter info about test server
    $("#loginDiv").show();
    $("#loginDiv input:password").val("");
    $("#loginDiv").removeClass("loginDivClass");
    $("#loginButton").remove();
    $("#password").val("");
    $("#editRunSettings").show();

    $("#loginDiv").dialog({
        width: 670,
        height: 545,
        modal: true,
        title: "Enter the GenePattern server to run tests on",
        buttons: {
            "Submit Tests": function()
            {
                if( $("#server").val() != "" && $("#username").val() != "")
                {
                    $("#runSettings").empty();
                    $("#runSettings").append("<p> Server: " + $("#server").val() + "</p>");
                    $("#runSettings").append("<p> Username: " + $("#username").val() + "</p>");
                    $("#settingsTab").tabs('option', 'active', 1);

                    runTests();
                    $(this).dialog("close");
                }
                else
                {
                    createErrorMsg("Server Login", "Please enter a server, username and/or password.");
                }
            },
            Cancel: function()
            {
                $(this).dialog("close");
            }
        }
    });
}


function runTests ()
{
    $.ajax({
        type: "POST",
        url: window.location.pathname + "AutomatedTest/runTests",
        data: { "server" : $("#server").val(),
                "username" : $("#username").val(),
                "password": $("#password").val(),
                "lsid": $(".moduleVersionSelect").val(),
                "paramSetGroupName":test_editor.currentParamSetGroup,
                "paramSetGroupLoc" : test_editor.currentParamSetGroupLoc,
                "gpUnitServerDir":test_editor.gpUnitServerDir,
                "gpUnitTimeout":test_editor.gpUnitTimeout,
                "gpUnitNumThreads":test_editor.gpUnitNumThreads,
                "gpUnitClient":test_editor.gpUnitClient
        },
        beforeSend:function()
        {
            //disable the run button
            $("span", $("#Run")).text("Running Tests...");
            $("#Run").attr("disabled", "disabled");
        },
        success: function(response) {
            var message = response["MESSAGE"];
            var error = response["ERROR"];

            if (error !== undefined && error !== null)
            {
                createErrorMsg("Run Tests", htmlEncode(error));
                return;
            }
            if (message !== undefined && message !== null) {
                createInfoMsg("Run Tests", htmlEncode(message));
                return;
            }
        },
        error: function (xhr, ajaxOptions, thrownError) {
            createErrorMsg("Run Tests", "Error status: " + xhr.status);
            if(thrownError != null && thrownError != "")
            {
                createErrorMsg("Run Tests", htmlEncode(thrownError));
            }
        },
        complete: function()
        {
            //enable the run button
            $("#Run").removeAttr("disabled");          
            $("span", $("#Run")).text("Run Tests");
        },
        dataType: "json"
    });
}
function loadAllParamSetsInfo(paramSetsGroupsByLoc)
{
    $("#savedParamSets").empty();

    var paramSetGroupsLocations = Object.keys(paramSetsGroupsByLoc);
    for(var i=0;i<paramSetGroupsLocations.length;i++)
    {
        var paramSetsGroups = paramSetsGroupsByLoc[paramSetGroupsLocations[i]];
        var paramSetsGroupNames = Object.keys(paramSetsGroups);
        var table = $("<table class='psetLocationTable'/>");

        var location = paramSetGroupsLocations[i];
        if(location.length > 60)
        {
            location = location.substring(0, 20) + "..." + location.substring(location.length-25, location.length);
        }

        var tableHeader = $("<td/>");
        tableHeader.append("<b>"  + location + " </b>");

        var deleteBtn = $("<img class='images' src='/automatrix/styles/images/delete-blue.png'/>");
        deleteBtn.button().click(function ()
        {
            var yes=confirm("Are you sure you want to delete this location?");
            if (yes)
            {
                var data = $(this).data("location");
                $(this).parents("table:first").remove();
                console.log("delete location: " + $(this).data("location"));
                removePSetGroupLocation(data);
            }
        });
        deleteBtn.data("location", paramSetGroupsLocations[i]);
        if(paramSetGroupsLocations[i].match("/automatrix\/param_sets$") == null)
        {
            tableHeader.append(deleteBtn);
        }
        var tableHeaderRow = $("<tr title='"+paramSetGroupsLocations[i]+"'/>");
        tableHeaderRow.append(tableHeader);
        table.append(tableHeaderRow);
        for(var t=0;t<paramSetsGroupNames.length;t++)
        {
            var tr = $("<tr/>");
            var td = $("<td/>");

            var versionText = "";
            var index = paramSetsGroups[paramSetsGroupNames[t]].lsid.lastIndexOf(":");
            if(index != -1)
            {
                versionText = " v. " + paramSetsGroups[paramSetsGroupNames[t]].lsid.substring(index+1,
                                                paramSetsGroups[paramSetsGroupNames[t]].lsid.length);
            }

            var moduleName = paramSetsGroups[paramSetsGroupNames[t]].module;
            var href = $("<a title='"+ moduleName + versionText +"' href='#'>" + paramSetsGroupNames[t] + "</a>");
            href.data("glocation", paramSetGroupsLocations[i]);
            href.click(function(event)
            {
                event.preventDefault();

                test_editor.currentParamSetGroup = $(this).text();
                test_editor.currentParamSetGroupLoc = $(this).data("glocation");

                $.ajax({
                    type: "POST",
                    url: window.location.pathname + "AutomatedTest/loadParamSets",
                    data: { "server" : test_editor.server,
                            "username" : test_editor.username,
                            "paramSetGroupName" : test_editor.currentParamSetGroup,
                            "paramSetGroupLoc" : test_editor.currentParamSetGroupLoc},
                    success: function(response) {
                        var message = response["MESSAGE"];
                        var error = response["ERROR"];
                        var paramSetGroup = response["param_sets_info"];

                        if (error !== undefined && error !== null)
                        {
                            createErrorMsg("Get Parameter Set", htmlEncode(error));
                            return;
                        }
                        if (message !== undefined && message !== null)
                        {
                            createInfoMsg("Get Parameter Set", htmlEncode(message));
                            return;
                        }

                        loadParamSetGroup(paramSetGroup);
                    },
                    error: function (xhr, ajaxOptions, thrownError) {
                        createErrorMsg("Get Parameter Set", "Error status: " + xhr.status);
                        if(thrownError != null && thrownError != "")
                        {
                            createErrorMsg("Get Parameter Set", htmlEncode(thrownError));
                        }
                    },
                    dataType: "json"
                });
            });
            td.append(href);
            tr.append(td);
            table.append(tr);
        }
        $("#savedParamSets").append(table);
    }
}

function loadParamSetGroup(paramSetGroup)
{
    parameterSets = paramSetGroup;
    viewParameterSets(paramSetGroup, true);
}

//add property to a parameter in the config settings
function addPropertyToParamConfig(pName, attribute, value)
{
    var paramConfig = pset_config[pName];

    if(paramConfig == null)
    {
        paramConfig = {};
    }

    paramConfig[attribute] = value;

    pset_config[pName] = paramConfig;
}

//add property to a parameter in the config settings
function getPropertyValueFromParamConfig(pName, attribute)
{
    var paramConfig = pset_config[pName];

    var value = null;
    if(paramConfig != null)
    {
        value = paramConfig[attribute];
    }

    return value;
}

function displayMainPage()
{
    if(mainLayout == null)
    {
        createErrorMsg("Initialization Error", "An error occurred while loading the page");
        return;
    }

    mainLayout.open('west');
    mainLayout.sizePane("west", "410");

    loadAllTasks();
}

function getRandomNum(min, max) {
    return (Math.random() * (max - min) + min).toFixed(2);
}

function getRandomInt (min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}


//get a parameter set with a specific id
function getParameterSet(id)
{
    var pSetArray = parameterSets["param_sets"];
    for(var t=0;t<pSetArray.length;t++)
    {
        var parameterSet = pSetArray[t];
        if(parameterSet["id"] == id)
        {
            return parameterSet;
        }
    }

    return null;
}

//get a parameter set with a specific id
function removeParameterSet(id)
{
    var pSetArray = parameterSets["param_sets"];
    for(var t=0;t<pSetArray.length;t++)
    {
        var parameterSet = pSetArray[t];
        if(parameterSet["id"] == id)
        {
            pSetArray.splice(t,1);
            return true;
        }
    }
    console.log("Could not delete the parameter set with id: " + id);
    createErrorMsg("Remove Parameter Set", "Could not delete the parameter set with id: " + id);
    return false;
}



//get a parameter set with a specific id
function updateParameterSet(id, paramSet)
{
    var pSetArray = parameterSets["param_sets"];
    for(var t=0;t<pSetArray.length;t++)
    {
        var parameterSet = pSetArray[t];
        if(parameterSet["id"] == id)
        {
            pSetArray[t] = paramSet;
            return true;
        }
    }
    console.log("Could not update parameter set with id: " + id);
    createErrorMsg("Update Parameter Set", "Could not update the parameter set with id: " + id);
    return false;
}

function extractFromJson(jsonObj, table, td)
{
    var keys = Object.keys(jsonObj);
    for(var k=0;k<keys.length;k++)
    {
        var value = jsonObj[keys[k]];

        if(value == undefined || value == null || value == "")
        {
            continue;
        }
        table.append("<tr>" + td + " <td>" +  keys[k] + ":</td></tr>");


        if($.isPlainObject(jsonObj[keys[k]]))
        {
            td += "<td/>";

            table = extractFromJson(jsonObj[keys[k]], table, td);
        }
        else
        {
            table.append("<tr>" + td + "<td/><td>" + jsonObj[keys[k]] + "</td></tr>");

        }
    }

    return table;
}

//update a given a test/assertion table listing id
function updateTestTable(id)
{
    var testsTable = $("#"+id+"Tests");
    testsTable.empty();
    testsTable.append("<thead><tr><th>Tests</th></tr></thead>");

    var parameterSet = getParameterSet(id);
    var tests = parameterSet["assertions"];
    var testsKeys = Object.keys(tests);

    console.log("keys: " + testsKeys);
    for(var q=0;q<testsKeys.length;q++)
    {
        if($.isArray(tests[testsKeys[q]]))
        {
            var filesTable = $("<table/>");
            filesTable.append("<tr><td>" + testsKeys[q] + "</td></tr>");
            var td = "<td/>";
            for(var t=0;t<tests[testsKeys[q]].length;t++)
            {
                var tObject =  tests[testsKeys[q]][t];
                if($.isPlainObject(tObject))
                {
                    filesTable = extractFromJson(tObject, filesTable, td);
                }
            }

            testsTable.append(filesTable);
        }
        else
        {
            testsTable.append("<tr><td>" + testsKeys[q]+ " = " + tests[testsKeys[q]] + "</td></tr>");
        }
    }
}

function addParameterSet( parameterSet)
{
    var pSetId = parameterSet["id"];
    var groupInfo = parameterSets["group_info"];

    var parametersChoiceMap = groupInfo["choices"];

    var pSetName = parameterSet["name"];

    if(pSetName == null || pSetName == undefined)
    {
        pSetName = parameterSet["id"];
    }

    var parameters = parameterSet["params"];
    var pSetDiv = $("<div class='pSetDiv'/>");
    var pSetActionDiv = $("<div class='pSetActionBar'/>");

    pSetDiv.append(pSetActionDiv);

    var editButton = $("<button class='Edit'>Edit</button>");
    editButton.data("pSetId", pSetId);
    editButton.button().click(function()
    {
        var editButtonMode =  $(this).text();
        var pSetId = editButton.data("pSetId");

        var pSetTable = $("#"+pSetId);
        pSetTable.addClass("highlightPSetEdit");
        pSetTable.find("tr").each(function()
        {
            var parameterName = $(this).find("td:first").text();
            var parameterValue = $(this).find("td:nth-child(2)").text();

            if(parameterName != null && parameterName != "")
            {
                parameterName = parameterName.replace(/ /g,'.');
                parameterName = parameterName.replace(/\*/g,'');

                var paramChoicesMap = groupInfo["choices"];
                var choiceMap = paramChoicesMap[parameterName];

                if(editButtonMode == "Edit")
                {
                    //if(type !=undefined && type == "choice")
                    if(choiceMap != null && choiceMap != undefined)
                    {

                        var choiceKeys = Object.keys(choiceMap);
                        var choiceSelect = $("<select class='editOptionValue'/>");

                        for(var t=0;t<choiceKeys.length;t++)
                        {
                            var choice = choiceKeys[t];

                            var option = $("<option/>");

                            var dValue = htmlEncode(choiceMap[choice]);
                            var defaultVal = groupInfo["defaults"][parameterName];
                            if(defaultVal == choice)
                            {
                                dValue += " (default)";
                            }
                            option.append(dValue);
                            option.val(htmlEncode(choice));
                            choiceSelect.append(option);

                            if(choiceMap[choice] == parameterValue)
                            {
                                choiceSelect.val(htmlEncode(choice));
                            }
                        }

                        $(this).find("td:nth-child(2)").empty();
                        $(this).find("td:nth-child(2)").append(choiceSelect);
                    }
                    else
                    {
                        $(this).find("td:nth-child(2)").empty();
                        $(this).find("td:nth-child(2)").append("<input class='editPSetValue' type='text' value='"
                                + htmlEncode(parameterValue) +"'/>");
                    }
                }
                else
                {
                    //Save the edited parameter settings
                    var paramSet = getParameterSet(pSetId);
                    var pNewValue = "";
                    var displayValue = "";

                    var selectValue = $(this).find("td:nth-child(2)").find(".editOptionValue").val();
                    var inputValue = $(this).find("td:nth-child(2)").find(".editPSetValue").val();

                    if(selectValue != undefined && selectValue != null)
                    {
                        pNewValue = htmlDecode(selectValue);
                        displayValue = choiceMap[pNewValue];
                    }

                    if(inputValue != undefined && inputValue != null)
                    {
                        pNewValue = inputValue;
                        displayValue = pNewValue;
                    }

                    var params = paramSet["params"];
                    for(var p=0;p<params.length;p++)
                    {
                        //update the value for the current parameter
                        if(params[p]["name"] == parameterName)
                        {
                            params[p]["value"] = pNewValue;
                            break;
                        }
                    }
                    paramSet["params"]  = params;

                    updateParameterSet(pSetId, paramSet);

                    $(this).find("td:nth-child(2)").empty();
                    $(this).find("td:nth-child(2)").text(displayValue);
                }
            }
        });

        if(editButtonMode == "Edit")
        {
            $("span", this).text("Save");
            $(this).siblings(".Cancel").first().show();
        }
        else
        {
            $("span", this).text("Edit");
            $(this).siblings(".Cancel").first().hide();
        }
    });
    pSetActionDiv.append(editButton);

    var deleteButton = $("<button class='Delete'>Delete</button>");
    deleteButton.data("pSetId", pSetId);
    deleteButton.button().click(function()
    {
        //remove parameter set from listing
        var parentDiv = $(this).parents(".pSetDiv");
        var pSetTable = parentDiv.find(".pSetTable");
        pSetTable.removeClass("highlightPSetEdit");
        var pSetId = deleteButton.data("pSetId");

        var parameterSet = getParameterSet(pSetId);
        var paramSetName = parameterSet.name;

        var yes=confirm("Are you sure you want to delete\n "
                + paramSetName +"?");
        if (yes)
        {
            removeParameterSet(pSetId);
            parentDiv.prev().remove();
            parentDiv.remove();
        }

    });
    pSetActionDiv.append(deleteButton);

    var cancelButton = $("<button class='floatLeft Cancel'>Cancel</button>");
    cancelButton.data("pSetId", pSetId);
    cancelButton.button().click(function()
    {
        //update to original settings
        var pSetId = cancelButton.data("pSetId");

        var pSetTable = $("#"+pSetId);
        pSetTable.removeClass("highlightPSetEdit");
        pSetTable.find("tr").each(function()
        {
            var paramSet = getParameterSet(pSetId);

            var parameterName = $(this).find("td:first").text();
            var displayValue = "";

            var paramChoicesMap = groupInfo["choices"];

            if(parameterName != null && parameterName != "")
            {
                parameterName = parameterName.replace(/ /g,'.');
                parameterName = parameterName.replace(/\*/g,'');

                var choiceMap = paramChoicesMap[parameterName];

                var params = paramSet["params"];
                for(var p=0;p<params.length;p++)
                {
                    //update the value for the current parameter
                    if(params[p]["name"] == parameterName)
                    {
                        if(choiceMap != undefined && choiceMap != null)
                        {
                            displayValue = choiceMap[params[p]["value"]];
                        }
                        else
                        {
                            displayValue = params[p]["value"];
                        }
                        break;
                    }
                }
                paramSet["params"]  = params;

                $(this).find("td:nth-child(2)").empty();
                $(this).find("td:nth-child(2)").text(displayValue);
            }
        });

        var editButton = $(this).siblings(".Edit").first();
        $("span", editButton).text("Edit");
        $(this).hide();
    });
    pSetActionDiv.append(cancelButton);
    cancelButton.hide();

    pSetDiv.append("<div class='pSetDescriptionFinalDiv'>Description: <span class='pSetDescriptionFinal'></span></div>");
    pSetDiv.find(".pSetDescriptionFinalDiv").hide();
    var pSetDescriptionEditDiv = $("<div class='pSetDescriptionEditDiv'/>");
    pSetDescriptionEditDiv.hide();

    pSetDiv.append(pSetDescriptionEditDiv);

    pSetDescriptionEditDiv.append("<textarea class='pSetDescriptionEdit'/>");
    pSetDescriptionEditDiv.append("<br/>");
    var saveDescriptionButton = $("<button class='saveDescription'>Save</button>");
    saveDescriptionButton.button().click(function()
    {
        $(this).parents(".pSetDescriptionEditDiv").hide();
        parameterSet["description"] = $(this).parents(".pSetDescriptionEditDiv").find(".pSetDescriptionEdit").val();
        $(this).parents(".pSetDiv").find(".pSetDescriptionFinal").text(parameterSet["description"]);

        if(parameterSet["description"].length > 0)
        {
            $(this).parents(".pSetDiv").find(".pSetDescriptionFinalDiv").show();
            $("span", $(this).parents(".pSetDiv").find(".descriptionButton")).text("Edit Description");
        }
        else
        {
            $("span", $(this).parents(".pSetDiv").find(".descriptionButton")).text("Add Description");
        }
    });
    pSetDescriptionEditDiv.append(saveDescriptionButton);

    var deleteDescriptionButton = $("<button>Delete</button>");
    deleteDescriptionButton.button().click(function()
    {
        $(this).parents(".pSetDescriptionEditDiv").find(".pSetDescriptionEdit").val("");
        $(this).parents(".pSetDescriptionEditDiv").find(".saveDescription").click();
    });
    pSetDescriptionEditDiv.append(deleteDescriptionButton);


    var cancelDescriptionButton = $("<button>Cancel</button>");
    cancelDescriptionButton.button().click(function()
    {
        $(this).parents(".pSetDescriptionEditDiv").hide();

        if(parameterSet["description"].length > 0) {
            $(this).parents(".pSetDiv").find(".pSetDescriptionFinalDiv").show();
        }
    });
    pSetDescriptionEditDiv.append(cancelDescriptionButton);

    var descriptionButton = $("<button class='descriptionButton'>Add Description</button>");
    if(parameterSet["description"] != undefined && parameterSet["description"] != null
        && parameterSet["description"] != "" && parameterSet["description"].length > 0)
    {
        descriptionButton = $("<button class='descriptionButton'>Edit Description</button>");
        pSetDiv.find(".pSetDescriptionFinal").append(parameterSet["description"]);
        pSetDiv.find(".pSetDescriptionFinalDiv").show();
    }
    descriptionButton.button().click(function()
    {
        $(this).parents(".pSetDiv").first().find(".pSetDescriptionFinalDiv").hide();
        $(this).parents(".pSetDiv").first().find(".pSetDescriptionEdit").val(
            $(this).parents(".pSetDiv").first().find(".pSetDescriptionFinal").text());
        $(this).parents(".pSetDiv").first().find(".pSetDescriptionEditDiv").show();

        $(this).addClass("hightlightButton");
    });

    pSetActionDiv.append(descriptionButton);

    var pSetTable = $("<table class='pSetTable' id='" + pSetId + "'/>");
    pSetTable.append("<thead><tr><th>Name</th><th>Value</th></tr></thead>");
    for(var k=0;k<parameters.length;k++)
    {
        var parametersObject = parameters[k];

        var pName = parametersObject["name"];
        var pNameWithSpace = pName.replace(/\./g,' ');

        if(groupInfo["altParamInfo"] != undefined && groupInfo["altParamInfo"] != null
            && groupInfo["altParamInfo"][pName] != undefined && groupInfo["altParamInfo"][pName] != null)
        {
            if(groupInfo["altParamInfo"][pName].name != undefined && groupInfo["altParamInfo"][pName].name != null
            && groupInfo["altParamInfo"][pName].name.length > 0)
            {
                pNameWithSpace = groupInfo["altParamInfo"][pName].name.replace(/\./g,' ');
            }
        }
        //set actual and display value to be the same for now
        var actualValue = parametersObject["value"];
        var value = parametersObject["value"];

        if(parametersChoiceMap[pName] != undefined
            && parametersChoiceMap[pName] != null)
        {
            //show the display value
            value = parametersChoiceMap[pName][value];
        }
        var optional = groupInfo["optional"].indexOf(pName) > -1;
        if(!optional)
        {
            pNameWithSpace += "*";
        }
        //check if this is the default value for this parameter
        //var defaultVal = getPropertyValueFromParamConfig(pName, "default");
        var defaultVal = groupInfo["defaults"][pName];
        var rowClass = "nondefaultValue";
        if(defaultVal == actualValue)
        {
            rowClass = "defaultValue";
        }

        pSetTable.append("<tr class='"+ rowClass +"'><td>" + pNameWithSpace + "</td>"
                    +   "<td>" + htmlEncode(value) +"</td></tr>");
    }
    pSetDiv.append(pSetTable);

    pSetDiv.append("<hr/>");

    var editTestsButton = $("<button>Edit</button>");
    editTestsButton.data("pSetId", pSetId);
    editTestsButton.button().click(function()
    {
        var editTestDialog = $("<div class='clear dialog'></div>");
        editTestDialog.dialog({
            title: 'Edit Tests/Assertions',
            autoOpen: true,
            height: 400,
            width: 550,
            create: function()
            {
                var addTest = $("<button>Add Test</button>");
                addTest.button().click(function()
                {
                    var tr = $("<tr/>");

                    var availableTests = $("<select name='tname'/>");

                    availableTests.append("<option value='jobStatus'>jobStatus</option>");
                    availableTests.append("<option value='numFiles'>numFiles</option>");
                    availableTests.append("<option value='exitCode'>exitCode</option>");
                    availableTests.append("<option value='files'>files</option>");

                    var nameTd = $("<td/>");
                    nameTd.append(availableTests);
                    tr.append(nameTd);

                    var valueTd = $("<td/>");
                    valueTd.append("<input name='tvalue' type='text'/>");
                    var deleteButton = $("<img class='images' src='/automatrix/styles/images/delete-blue.png'/>");

                    deleteButton.button().click(function()
                    {
                        $(this).parents("tr:first").remove();
                    });

                    valueTd.append(deleteButton);
                    tr.append(valueTd);

                    $(this).parents("div:first").find("table").append(tr);

                    availableTests.combobox({
                        select: function () {
                            if ($(this).val() == "files") {
                                $(this).parents("tr").first().find("input[name='tvalue']")
                                    .replaceWith("<textarea name='tvalue' type='text'/>");
                            }
                        }
                    });

                });

                $(this).append(addTest);
                var table = $("<table class='editTestTable'>\
                            <tr>  \
                                <th>Test Name</th> \
                                <th>Value</th> \
                            </tr>  \
                        </table>");
                $(this).prepend(table);

                var tests = parameterSet["assertions"];
                var testsKeys = Object.keys(tests);

                for(var q=0;q<testsKeys.length;q++)
                {
                    //first add a row of new tests
                    addTest.click();

                    //now set their name and value
                    var tr = table.find("tr:last");

                    if(testsKeys[q] != "files")
                    {
                        //update value of the hidden select field and also the new combobox field
                        tr.find("select[name='tname']").val(testsKeys[q]);
                        tr.find(".custom-combobox-input").val(testsKeys[q]);
                        tr.find("input[name='tvalue']").val(tests[testsKeys[q]]);
                    }
                    else
                    {
                        //update value of the hidden select field and also the new combobox field
                        tr.find("select[name='tname']").val(testsKeys[q]);
                        tr.find(".custom-combobox-input").val(testsKeys[q]);

                        var textarea = $("<textarea name='tvalue'/>");
                        textarea.val(JSON.stringify(tests[testsKeys[q]]));
                        tr.find("input[name='tvalue']").replaceWith(textarea);
                    }

                }

                table.find("tbody").sortable();
            },
            close: function()
            {
                $( this ).dialog( "destroy" );
            },
            buttons: {
                "OK": function() {
                    parameterSet["assertions"] = {};
                    $(this).find("tr").each(function()
                    {
                        var tname = $(this).find("select[name='tname']").val();
                        var tvalue = $(this).find("input[name='tvalue']").val();

                        if(tname == "files")
                        {
                            tvalue = $(this).find("textarea[name='tvalue']").val();
                            tvalue = JSON.parse(tvalue);
                        }

                        if((tname == undefined && tvalue == undefined)
                            || (tname == "" && tvalue==""))
                        {
                            return;
                        }
                        parameterSet["assertions"][tname] = tvalue;
                        updateTestTable(pSetId);
                    });

                    $(this).dialog("destroy");
                },
                "Cancel": function() {
                    $( this ).dialog( "destroy" );
                }
            },
            resizable: true
        });
    });

    pSetDiv.append(editTestsButton);
    //show tests/assertions that will be applied to parameter set
    var testsTable = $("<table class='testTable' id='" + pSetId + "Tests' />");
    pSetDiv.append(testsTable);

    //add ability to toggle visibility of parameter sets
    var pSetHeading = $("<p class='pSetHeading'>" + pSetName +"</p>");
    //$(".middle-center").append(pSetHeading);
    var imagecollapse = $("<img class='imgcollapse' src='styles/images/black_section_collapsearrow.png' alt='some_text' width='11' height='11'/>");
    var imageexpand = $("<img class='imgexpand' src='styles/images/black_section_expandarrow.png' alt='some_text' width='11' height='11'/>");

    pSetHeading.prepend(imageexpand);
    pSetHeading.prepend(imagecollapse);

    pSetHeading.children(".imgcollapse").toggle();

    //pSetHeading.next(".pSetDiv").data("visible", true);
    pSetHeading.click(function()
    {
        $(this).next(".pSetDiv").slideToggle(340);
        $(this).children(".imgcollapse:first").toggle();
        $(this).children(".imgexpand:first").toggle();
    });

    var divAccordion = $("<div class='pSetCollapseDiv'/>");
    divAccordion.append("<h3>" + pSetName + "</h3>");
    divAccordion.append(pSetDiv);
    divAccordion.accordion({
        collapsible: true,
        heightStyle: "content"
    });
    $(".middle-center").append(divAccordion);

    updateTestTable(pSetId);
}

function updateParameterSetGroup()
{
    var lsidNoVersion = test_editor.lsid;

    var lastIndex = test_editor.lsid.lastIndexOf(":");

    lsidNoVersion = lsidNoVersion.substring(0, lastIndex);


    //retrieve latest version of module
    var REST_ALL_TASKS = "/rest/v1/tasks/";

    $.ajax({
        type: "GET",
        url: test_editor.server + REST_ALL_TASKS + lsidNoVersion,
        xhrFields: {
            withCredentials: true
        },
        success: function(response) {
            handleParameterSetGroupUpdate(response);
        },
        error: function (xhr, ajaxOptions, thrownError) {
            createErrorMsg("Load Module", "Error status: " + xhr.status);
            if(thrownError != null && thrownError != "")
            {
                createErrorMsg("Load Module", htmlEncode(thrownError));
            }
        },
        dataType: "json"
    });
}

function handleParameterSetGroupUpdate(module)
{
    var updatedParams = {};

    var parameterInfos = module["params"];
    var curParameterNameSelect = $("<select class='pNameSelect'/>");
    curParameterNameSelect.append("<option value='' selected='selected'>Select new parameter</option>");

    //get data about the parameters for the latest module
    var currentParamsObj = {};
    for(var t=0;t<parameterInfos.length;t++)
    {
        var paramsObject = parameterInfos[t];
        var paramName = Object.keys(paramsObject);
        if(paramName.length == 1) {
            var parameter = paramsObject[paramName];

            var obj = {};
            obj.defaultValue = parameter.attributes.default_value;

            var optional = true;
            if (parameter.attributes.optional.length <= 0) {
                optional = false;
            }

            obj.optional = optional;
            var choiceInfo = parameter.choiceInfo;

            var defaultFound = false;
            var firstChoiceValue = null;
            var choiceMap = {};
            //get display value if this is a choice list
            //the test of displaying values of 
            if(choiceInfo != undefined && choiceInfo != null && choiceInfo != "")
            {
                var result = choiceInfo.choices;

                for(var j=0;j<result.length;j++)
                {
                    var displayValue = result[j].label;
                    var value = result[j].value;


                    if(value == parameter.attributes.default_value)
                    {
                        defaultFound = true;
                    }
                    //if no default value is specified for the
                    //then the first choice will be used
                    if(j==0)
                    {
                        firstChoiceValue = value;
                    }

                    choiceMap[value] = displayValue;
                }

                if(!defaultFound)
                {
                    parameter.attributes.default_value = firstChoiceValue;
                }

                //if there is no default value set that to
                if (obj.defaultValue == undefined
                    || obj.defaultValue == null
                    || choiceMap[obj.defaultValue] == undefined
                    || choiceMap[obj.defaultValue] == null) {
                    obj.defaultValue = firstChoice;
                }

                obj.choiceMap = choiceMap;

            }

            currentParamsObj[paramName] = obj;
            curParameterNameSelect.append("<option value='" + paramName + "'>" + paramName + " </option>");
        }
    }

    //now find parameters that need to be updated
    var paramsToUpdateObj = {};
    for(var i=0;i<parameterSets["param_sets"].length;i++)
    {
        var parameterSet = parameterSets["param_sets"][i];
        var parameters = parameterSet["params"];
        for(var k=0;k<parameters.length;k++)
        {
            var pName = parameters[k].name;
            var value = parameters[k].value;

            var pObj = paramsToUpdateObj[pName];
            if(pObj == undefined || pObj == null)
            {
                pObj = {};
            }

            var updateValueList = pObj.updateValueList;
            if(updateValueList == undefined || updateValueList == null)
            {
                updateValueList = [];
                pObj.updateValueList = updateValueList;
            }

            // Check if this parameter exists in the latest
            // version of the task
            if(currentParamsObj[pName] == undefined
                && currentParamsObj[pName] == null)
            {
                //if we get here then the parameter is missing from the latest version
                pObj.missing = true;

                if($.inArray(value, pObj.updateValueList ) == -1)
                {
                    pObj.updateValueList.push(value);
                }

                paramsToUpdateObj[pName] = pObj;
            }
            else
            {
                pObj.missing = false;
                //now check that its specified value is a valid choice
                //which only applies for choice parameters
                if(currentParamsObj[pName].choiceMap != undefined
                    && currentParamsObj[pName].choiceMap != null
                    && (currentParamsObj[pName].choiceMap[value] == undefined
                    || currentParamsObj[pName].choiceMap[value] == null) )
                {
                    //if we get here then the value is not valid
                    if($.inArray(value, pObj.updateValueList) == -1)
                    {
                        pObj.updateValueList.push(value);
                        paramsToUpdateObj[pName] = pObj;
                    }
                }
            }
        }
    }

    //open dialog to update parameters
    var updateDiv = $("<div id='updateDiv'/>");
    var updateParamsTable = $("<table id='updateParamsTable'/>");
    var paramsForm = $("<form id='paramsForm'/>");
    paramsForm.append(updateParamsTable);
    updateDiv.append(paramsForm);

    //keep track of parameter updates
    var finalParameterUpdates = {};

    var paramsToUpdateNameKeys = Object.keys(paramsToUpdateObj);
    for(var y=0;y<paramsToUpdateNameKeys.length;y++)
    {
        var pObj = paramsToUpdateObj[paramsToUpdateNameKeys[y]];

        var className = "odd";
        if(y%2 != 0)
        {
            className ="even";
        }

        var tr = $("<tr/>");
        updateParamsTable.append(tr);

        tr.addClass(className);
        tr.append("<td>" + paramsToUpdateNameKeys[y] + "</td>");
        if(pObj.missing)
        {
            var missingSelect = $("<select name='"+ paramsToUpdateNameKeys[y] +"'/>");
            missingSelect.append("<option value='remove'>remove</option>");
            missingSelect.append("<option value='replace'>replace with</option>");

            var newPSelect = curParameterNameSelect.clone();
            newPSelect.change(function()
            {
                var pnameOld = $(this).data("old_pname");
                var pnameNew = $(this).val();
                var prevPname = $(this).data("prevVal");

                $(".pNameSelect").children('option').each(function()
                {
                    if ( $(this).val() == pnameNew )
                    {
                        $(this).attr('disabled','disabled');
                    }

                    if($(this).val() == prevPname)
                    {
                        $(this).removeAttr("disabled");
                    }
                });

                //remove any new parameter names and values
                var fObj = finalParameterUpdates[pnameOld];
                if(fObj == undefined || fObj == null)
                {
                    fObj = {};
                }

                fObj.newName = pnameNew;
                fObj.optional = currentParamsObj[pnameNew].optional;
                fObj.defaultValue = currentParamsObj[pnameNew].defaultValue;
                fObj.choiceMap = currentParamsObj[pnameNew].choiceMap;
                fObj.newValueMap = {};
                //if there are values to update
                if(paramsToUpdateObj[pnameOld].updateValueList != undefined
                    && paramsToUpdateObj[pnameOld].updateValueList != null)
                {
                    //list old parameter values to change
                    for(var v=0;v<paramsToUpdateObj[pnameOld].updateValueList.length;v++)
                    {
                        var valuesTr = $("<tr/>");
                        valuesTr.append("<td/>");
                        var valuesTd = $("<td/>");
                        valuesTd.append("Change '" + paramsToUpdateObj[pnameOld].updateValueList[v] + "' to");

                        if(currentParamsObj[pnameNew].choiceMap != undefined
                            && currentParamsObj[pnameNew].choiceMap != null)
                        {
                            var newValuesSelect = $("<select/>");

                            var valuesNewKeys = Object.keys(currentParamsObj[pnameNew].choiceMap);
                            for(var n=0;n < valuesNewKeys.length;n++)
                            {
                                newValuesSelect.append("<option value='" + valuesNewKeys[n] + "'>" +
                                    currentParamsObj[pnameNew].choiceMap[valuesNewKeys[n]] + "</option>");
                            }

                            newValuesSelect.change(function()
                            {
                                var pname = $(this).data("pnameOld");

                                var valueMap = finalParameterUpdates[pname].newValueMap;

                                //add mapping of old parameter value to new parameter value
                                valueMap[paramsToUpdateObj[pnameOld].updateValueList[v]] = $(this).val();
                                finalParameterUpdates[pname].newValueMap = valueMap;
                            });

                            newValuesSelect.data("pnameOld", pnameOld);
                            newValuesSelect.data("valueOld", paramsToUpdateObj[pnameOld].updateValueList[v]);

                            valuesTd.append(newValuesSelect);
                        }
                        else
                        {
                            var newValueField = $("<input type='text'/>");
                            newValueField.data("pnameOld", pnameOld);
                            newValueField.data("valueOld", paramsToUpdateObj[pnameOld].updateValueList[v]);

                            newValueField.change(function()
                            {
                                var pname = $(this).data("pnameOld");

                                var valueMap = finalParameterUpdates[pname].newValueMap;

                                //add mapping of old parameter value to new parameter value
                                valueMap[paramsToUpdateObj[pnameOld].updateValueList[v]] = $(this).val();
                                finalParameterUpdates[pname].newValueMap = valueMap;
                            });
                            valuesTd.append(newValueField);
                        }
                        valuesTr.append(valuesTd);
                        $(this).parents("tr:first").after(valuesTr);
                    }
                }

                finalParameterUpdates[pnameOld] = fObj;

                //add new parameter name
                $(this).data("prevVal", pnameNew);
            });
            newPSelect.data("old_pname", paramsToUpdateNameKeys[y]);
            newPSelect.hide();

            missingSelect.change(function()
            {
                $(this).next(".pSelect").remove();
                if($(this).val() == "replace")
                {
                    $(this).next().show();
                }
                else
                {
                    //remove any new parameter names and values;
                    finalParameterUpdates[$(this).attr('old_pname')] = {};
                }
            });

            var tdMissing = $("<td/>");
            tdMissing.append(missingSelect);
            missingSelect.after(newPSelect);
            tr.append(tdMissing);
        }
        else
        {
            //if we get here then the parameter values are no longer valid and need to be changed
            if(paramsToUpdateObj[paramsToUpdateNameKeys[y]].updateValueList != undefined
                && paramsToUpdateObj[paramsToUpdateNameKeys[y]].updateValueList != null)
            {
                //list old parameter values to change
                for(var v=0;v<paramsToUpdateObj[paramsToUpdateNameKeys[y]].updateValueList.length;v++)
                {
                    var valuesTr = $("<tr/>");
                    valuesTr.append("<td/>");
                    var valuesTd = $("<td/>");

                    valuesTd.append("Change '" + paramsToUpdateObj[paramsToUpdateNameKeys[y]].updateValueList[v]
                        + "' to ");
                    var newValueField;
                    if(currentParamsObj[paramsToUpdateNameKeys[y]].choiceMap == undefined
                        || currentParamsObj[paramsToUpdateNameKeys[y]].choiceMap == null)
                    {
                        newValueField = $("<input type='text'/>");
                    }
                    else
                    {
                        newValueField = $("<select/>");
                        var valuesNewKeys = Object.keys(currentParamsObj[paramsToUpdateNameKeys[y]].choiceMap);
                        for(var n=0;n < valuesNewKeys.length;n++)
                        {
                            newValueField.append("<option value='" + valuesNewKeys[n] + "'>" +
                                currentParamsObj[paramsToUpdateNameKeys[y]].choiceMap[valuesNewKeys[n]] + "</option>");
                        }
                    }

                    //add the required class so validation will fail if this field is empty
                    newValueField.addClass("required");
                    newValueField.data("pnameOld", paramsToUpdateNameKeys[y]);
                    newValueField.data("valueOld", paramsToUpdateObj[paramsToUpdateNameKeys[y]].updateValueList[v]);
                    newValueField.change(function()
                    {
                        var pNewObj = finalParameterUpdates[newValueField.data("pnameOld")];
                        if(pNewObj == undefined || pNewObj == null)
                        {
                            pNewObj = {};
                            pNewObj.newValueMap = {};
                        }
                        pNewObj.newValueMap[$(this).data("valueOld")] = $(this).val();

                        finalParameterUpdates[$(this).data("pnameOld")] = pNewObj;
                    });

                    valuesTd.append(newValueField);
                    if(v == 0)
                    {
                        tr.append(valuesTd);
                    }
                    else
                    {
                        valuesTr.append(valuesTd);
                        tr.after(valuesTr);
                    }
                }
            }
        }
    }

    if(paramsToUpdateNameKeys.length > 0) {
        updateDiv.dialog(
            {
                modal: true,
                title: "Update ParameterSets",
                minWidth: 600,
                buttons: {
                    Cancel: function () {
                        $(this).dialog("close");
                    },
                    "Update": function () {

                        $("#paramsForm").validate();

                        //copy the old parameter sets group obj
                        var groupInfo = parameterSets["group_info"];

                        //update the module lsid and name
                        groupInfo["lsid"] = module["lsid"];
                        groupInfo["module"] = module["name"];

                        //update the parameter sets
                        for (var i = 0; i < parameterSets["param_sets"].length; i++) {
                            var numParamsRemoved = 0;
                            var numNewParameters = 0;
                            var newParamsList = [];
                            var newParamNamesList = [];
                            var parameterSet = parameterSets["param_sets"][i];
                            var parameters = parameterSet["params"];
                            for (var k = 0; k < parameters.length; k++) {
                                var pName = parameters[k].name;
                                var value = parameters[k].value;

                                //check if parameter is removed
                                if (finalParameterUpdates[pName] == undefined
                                    && finalParameterUpdates[pName] == null
                                    && currentParamsObj[pName] == undefined
                                    && currentParamsObj[pName] == null) {
                                    numParamsRemoved++;
                                    continue;
                                }

                                //check if parameter needs to be updated
                                if (finalParameterUpdates[pName] != undefined
                                    && finalParameterUpdates[pName] != null) {
                                    if (finalParameterUpdates[pName].newValueMap[parameters[k].value] != undefined
                                        && finalParameterUpdates[pName].newValueMap[parameters[k].value] != null) {
                                        parameters[k].value = finalParameterUpdates[pName].newValueMap[parameters[k].value];
                                    }

                                    if (finalParameterUpdates[pName].newName != undefined
                                        && finalParameterUpdates[pName].newName != null) {
                                        parameters[k].name = finalParameterUpdates[pName].newName;
                                    }
                                }

                                //keep track of just the names to use for checking for
                                //any new parameters
                                newParamNamesList.push(parameters[k].name);
                                newParamsList.push(parameters[k]);
                            }

                            //add any new parameters
                            var curParamNames = Object.keys(currentParamsObj);
                            for (var c = 0; c < curParamNames.length; c++) {
                                if ($.inArray(curParamNames[c], newParamNamesList) == -1) {
                                    numNewParameters++;
                                    var newPObj = {};
                                    newPObj.name = curParamNames[c];
                                    newPObj.value = "";
                                    if (currentParamsObj[curParamNames[c]].defaultValue != undefined
                                        && currentParamsObj[curParamNames[c]].defaultValue != null) {
                                        newPObj.value = currentParamsObj[curParamNames[c]].defaultValue;
                                    }

                                    newParamsList.push(newPObj);
                                }
                                //if this is a choice list then add the list of choices to group info
                                if (currentParamsObj[curParamNames[c]].choiceMap != undefined
                                    && currentParamsObj[curParamNames[c]].choiceMap != null) {
                                    if (groupInfo["choices"] == undefined
                                        || groupInfo["choices"] == null) {
                                        groupInfo["choices"] = {};
                                    }
                                    groupInfo["choices"][curParamNames[c]] = currentParamsObj[curParamNames[c]].choiceMap;
                                }

                                //check if this parameter is optional
                                if (currentParamsObj[curParamNames[c]].optional) {
                                    if ($.inArray(curParamNames[c], groupInfo["optional"]) == -1) {
                                        groupInfo["optional"].push(curParamNames[c]);
                                    }
                                }

                                //save the default values
                                if (currentParamsObj[curParamNames[c]].defaultValue != undefined
                                    && currentParamsObj[curParamNames[c]].defaultValue != null) {
                                    groupInfo["defaults"][curParamNames[c]] = currentParamsObj[curParamNames[c]].defaultValue;
                                }
                            }

                            parameterSet["params"] = newParamsList;
                        }

                        viewParameterSets(parameterSets, false);
                        createInfoMsg("Update Parameter Set", "Update complete: " + "\n" + numParamsRemoved + " parameters removed"
                            + "\n" + numNewParameters + " parameters added");
                        $(this).dialog("close");
                    }
                }
            });
    }
    else
    {
        createInfoMsg("Update Parameter Set", "No Parameters to update");
    }
}

function viewParameterSets(pGroupObj, enableRun)
{
    enableRun = typeof enableRun !== 'undefined' ? enableRun : false;
    //clear the center panel
    $(".middle-center").empty();
    $(".middle-south").empty();
    $("#groupSetDiv").remove();

    centerInnerLayout.open('north');

    var actionBarDiv = $("<div class='actionBar'/>");
    var updateButton = $("<button id='updateBtn' type='button'> Update </button>");
    updateButton.button().click(function()
    {
        //update parameter set to the lastest version of the module
        updateParameterSetGroup();
    });
    updateButton.attr("disabled", "disabled");

    actionBarDiv.append(updateButton);

    var togglePSetButton = $("<button id='updateBtn' type='button'>Collapse All</button>");
    togglePSetButton.button().click(function()
    {
        var toggleMode =  $(this).text().trim();

        if(toggleMode === "Collapse All")
        {
            $(".pSetCollapseDiv").each(function()
            {
                $(this).accordion( "option", "active", false);
            });

            $("span", this).text("Expand All");
        }
        else
        {
            $(".pSetCollapseDiv").each(function()
            {
                $(this).accordion( "option", "active", 0);
            });

            $("span", this).text("Collapse All");
        }
    });
    actionBarDiv.append(togglePSetButton);

    $(".middle-center").append(actionBarDiv);
    
    var paramGroupSetDiv = $("<div id='groupSetDiv'/>");
    paramGroupSetDiv.append("<label class='label' for='paramSetGroupName'> Name of parameter set group*: </label>");
    paramGroupSetDiv.append("<input id='paramSetGroupName' type='text'/>");

    //add check mark to indicate whether to overwrite the tests
    paramGroupSetDiv.append("<input type='checkbox' name='overwriteTests' id='overwriteTests' checked='true'/>");
    paramGroupSetDiv.append("<label class='label' for='overwriteTests'>overwrite existing tests</label>");
    $(document).on("change", "#overwriteTests", function()
    {
        if(!$(this).is(':checked'))
        {
            overwriteTests = false;
        }
        else
        {
            overwriteTests = true;
        }
    });

    $(".middle-north").empty();
    $(".middle-north").append(paramGroupSetDiv);
    centerInnerLayout.open("north");

    $('#paramSetGroupName').change(function()
    {
        var groupInfo = pGroupObj["group_info"];

        if(groupInfo == null)
        {
            groupInfo = {};
            parameterSets["group_info"] = groupInfo;
        }

        groupInfo["name"] = $(this).val();
    });

    var groupInfo = pGroupObj["group_info"];
    if(groupInfo["name"] != undefined)
    {
        $('#paramSetGroupName').val(groupInfo["name"]);    
    }

    test_editor.lsid = groupInfo["lsid"];
    test_editor.module = groupInfo["module"];

    var index = test_editor.lsid.lastIndexOf(":");
    if(index == -1)
    {
        createErrorMsg("Module Versions", "An error occurred while loading module versions.\nInvalid lsid: " + $(this).val());
    }
    var versionnum = test_editor.lsid.substring(index+1, test_editor.lsid.length);

    paramGroupSetDiv.prepend("<div class='header module_header'>" + test_editor.module + " v. " + versionnum + "</div>");

    centerInnerLayout.sizePane("north", "87");
    
    //Add control for adding parameter sets
    var pSetControlDiv = $("<div/>");
    /*var addPSetButton = $("<button>Add Parameter Set</button>");
    addPSetButton.button().click(function()
    {
        var paramSet = parameterSets["param_sets"];

        //grab an existing param set and reset to default values
        paramSet = paramSet[Object.keys(paramSet)[1]];
        paramSet["name"] = "New Parameter Set";

        addParameterSet(parameterSets["param_sets"].length, paramSet);
    });

    pSetControlDiv.append(addPSetButton); */
    $(".middle-center").append(pSetControlDiv);
    //Display inidividual parameter sets
    var pSetArray = pGroupObj["param_sets"];

    for(var t=0;t<pSetArray.length;t++)
    {
        var parameterSet = pSetArray[t];
        addParameterSet(parameterSet);
    }

    $(".middle-south").append("<span class='floatLeft'><b>Total tests: " + pSetArray.length + "</b></span>");
    //add button to save test
    var saveButton = $("<button>Save Tests</button>");

    saveButton.button().click(function()
    {
        var pGroupName = $("#paramSetGroupName").val();

        if(pGroupName != null && pGroupName != "")
        {
            saveTests();
        }
        else
        {
            createErrorMsg("Save Parameter Set", "Please specify the name of the parameter set group");
        }
    });
    $(".middle-south").append(saveButton);
    centerInnerLayout.open('south');

    var runButton = $("<button id='Run'>Run Tests</button>");
    runButton.button().click(function ()
    {
        //check if there are parameters sets currently in edit mode
        if($(".highlightPSetEdit").length == 0)
        {
            configureAndRunTests();
        }
        else
        {
            createErrorMsg("Run Tests", "There are edits " +
                "to parameter sets that have not been saved");
        }

    });

    runButton.attr("disabled", "disabled");
    $(".middle-south").append(runButton);

    toggleRunButton(enableRun);
}


function toggleRunButton(enableRun)
{
    if(enableRun)
    {
        $("#Run").removeAttr("disabled");
        $("#updateBtn").removeAttr("disabled");
    }
    else
    {
        $("#Run").attr("disabled", "disabled");
        $("#updateBtn").attr("disabled", "disabled");
    }
}

function saveTests()
{
    $.ajax({
        type: "POST",
        url: window.location.pathname + "AutomatedTest/saveTests",
        data:
        {
            "paramSets": JSON.stringify(parameterSets),
            "overwrite": overwriteTests
        },
        success: function(response) {
            var message = response["MESSAGE"];
            var error = response["ERROR"];
            if (error !== undefined && error !== null)
            {
                createErrorMsg("Save Parameter Sets", htmlEncode(error));
                return;
            }
            if (message !== undefined && message !== null) {
                createInfoMsg("Save Parameter Sets", htmlEncode(message));
            }

            loadAllParamSetGroups();

            test_editor.currentParamSetGroup = parameterSets["group_info"].name;
            //enable the run test button
            toggleRunButton(true);            
        },
        error: function (xhr, ajaxOptions, thrownError) {
            createErrorMsg("Save Parameter Sets", "Error status: " + xhr.status);
            if(thrownError != null && thrownError != "")
            {
                createErrorMsg("Save Parameter Sets", htmlEncode(thrownError));
            }
        },
        dataType: "json"
    });
}

$.widget( "custom.combobox", {
    _create: function() {
        this.wrapper = $( "<span>" )
            .addClass( "custom-combobox" )
            .insertAfter( this.element );

        this.element.hide();
        this._createAutocomplete();
        this._createShowAllButton();
    },

    _createAutocomplete: function() {
        var selected = this.element.children( ":selected" ),
            value = selected.val() ? selected.text() : "";

        this.input = $( "<input>" )
            .appendTo( this.wrapper )
            .val( value )
            .attr( "title", "" )
            .addClass( "custom-combobox-input ui-widget ui-widget-content ui-corner-left" )
            .autocomplete({
                delay: 0,
                minLength: 0,
                source: $.proxy( this, "_source" )
            })
            .tooltip({
                tooltipClass: "ui-state-highlight"
            });

        this._on( this.input, {
            autocompleteselect: function( event, ui ) {
                ui.item.option.selected = true;
                this._trigger( "select", event, {
                    item: ui.item.option
                });
            },

            autocompletechange: "_removeIfInvalid"
        });
    },

    _createShowAllButton: function() {
        var input = this.input,
            wasOpen = false;

        $( "<a>" )
            .attr( "tabIndex", -1 )
            //.attr( "title", "Show All Items" )
            .tooltip()
            .appendTo( this.wrapper )
            .button({
                icons: {
                    primary: "ui-icon-triangle-1-s"
                },
                text: false
            })
            .removeClass( "ui-corner-all" )
            .addClass( "custom-combobox-toggle ui-corner-right" )
            .mousedown(function() {
                wasOpen = input.autocomplete( "widget" ).is( ":visible" );
            })
            .click(function() {
                input.focus();

                // Close if already visible
                if ( wasOpen ) {
                    return;
                }

                // Pass empty string as value to search for, displaying all results
                input.autocomplete( "search", "" );
            });
    },

    _source: function( request, response ) {
        var matcher = new RegExp( $.ui.autocomplete.escapeRegex(request.term), "i" );
        response( this.element.children( "option" ).map(function() {
            var text = $( this ).text();
            if ( this.value && ( !request.term || matcher.test(text) ) )
                return {
                    label: text,
                    value: text,
                    option: this
                };
        }) );
    },

    _removeIfInvalid: function( event, ui ) {

        // Nothing to do if an item was selected except trigger a chang event
        if (! ui.item )
        {
            // Search for a match (case-insensitive)
            var value = this.input.val(),
                valueLowerCase = value.toLowerCase(),
                valid = false;
            this.element.children( "option" ).each(function() {
                if ( $( this ).text().toLowerCase() === valueLowerCase ) {
                    this.selected = valid = true;
                    return false;
                }
            });

            // Found a match, nothing to do
            if ( valid ) {
                return;
            }
            else
            {    //Marc -- added code to allow unlisted values to be specified
                this.element.append("<option value='" + value + "'>" + value + " </option>");
                this.element.val(value);
                valid = true;
            }

            /*Marc -- Commented out to allow unlisted values to be specified
            // Remove invalid value
            this.input
                .val( "" )
                .attr( "title", value + " didn't match any item" )
                .tooltip( "open" );
            this.element.val( "" );
            this._delay(function() {
                this.input.tooltip( "close" ).attr( "title", "" );
            }, 2500 );
            this.input.data( "ui-autocomplete" ).term = "";
            */
        }
        this._trigger( "change", event, {
            item: ui.item.option
        });
    },

    _destroy: function() {
        this.wrapper.remove();
        this.element.show();
    }
});

function createInfoMsg(title, message)
{
    var infoDiv = $("<div/>");
    var infoContents = $('<p/>');
    infoContents.append(message);
    infoDiv.append(infoContents);

    infoDiv.dialog({
        title: title,
        width: 480,
        buttons: {
            "OK": function()
            {
                $(this).dialog("close");
            }
        }
    });
}

function createErrorMsg(title, message)
{
    var errorDiv = $("<div/>");
    var errorContents = $('<p> <span class="ui-icon ui-icon-alert" style="float: left; margin-right: .3em;"></span>'
     + '<strong>Error: </strong></p>');
    errorContents.append(message);
    errorDiv.append(errorContents);

    errorDiv.dialog({
        title: title,
        width: 480,
        buttons: {
            "OK": function()
            {
                $(this).dialog("close");
            }
        }
    });
}

function updateModuleVersions(versions)
{
    if(versions == undefined || versions == null)
    {
        return;
    }

    var index = test_editor.lsid.lastIndexOf(":");
    if(index == -1)
    {
        createErrorMsg("Update Module Version", "An error occurred while loading module versions.\nInvalid lsid: " + $(this).val());
    }

    $(".moduleVersionSelect").empty();

    var lsidNoVersion = test_editor.lsid.substring(0, index);

    for(var v=0;v<versions.length;v++)
    {
        var version = versions[v];

        $(".moduleVersionSelect").append("<option value='" + lsidNoVersion + ":" + version +"'>" + version + "</option>")
    }


    $(".moduleVersionSelect").multiselect("refresh");
}

$(document).ready(function()
{
    $("#editRunSettings").hide();

    $("input[name='gpUnitServerDir']").change(function()
    {
        if($(this).val() != "")
        {
            test_editor.gpUnitServerDir = $(this).val();
        }
    });

    $("input[name='gpTimeout']").change(function()
    {
        if($(this).val() != "")
        {
            test_editor.gpUnitTimeout = $(this).val();
        }
    });

    $("input[name='gpNumThreads']").change(function()
    {
        if($(this).val() != "")
        {
            test_editor.gpUnitNumThreads = $(this).val();
        }
    });

    $("input[name='gpUnitClient'][value='REST']").attr('checked', 'checked');
    $("#gpUnitClient").buttonset();
    $("input[name='gpUnitClient']").change(function(event)
    {
        if($(this).val() != null && $(this).val() != undefined && $(this).val() != "")
        {
            test_editor.gpUnitClient = $(this).val();
        }
    });

    test_editor.gpUnitClient =  $("input[name='gpUnitClient']").val();

    var refreshParamSetsBtn = $("<button>Refresh</button>");
    refreshParamSetsBtn.button().click(function()
    {
        loadAllParamSetGroups();
    });

    var addParamSetsLocBtn = $("<button>Add Search Locations</button>");
    addParamSetsLocBtn.button().click(function()
    {
        var div = $("<div class='spacing'/>");
        var newSearchLocation = $("<input id='pSetSearchLocation' name='pSetSearchLocation' type='text'/>");
        div.append("<label for='pSetSearchLocation'>Enter directory path</label>  ");
        div.append(newSearchLocation);
        div.dialog({
            title: "Add Parameter Set Group Search Location",
            width: 550,
            buttons: {
                "Add": function()
                {
                    var newLoc = $("#pSetSearchLocation").val();
                    console.log("search location: " + newLoc);
                    if(newLoc != undefined && newLoc != "")
                    {
                        addPSetGroupLocation(newLoc);
                        loadAllParamSetGroups();
                    }

                    $(this).dialog("close");
                    $(this).remove();
                },
                "Cancel": function()
                {
                    $(this).dialog("close");
                    $(this).remove();
                }

            }
        });
    });

    $("#savedParamsSetsControls").append(refreshParamSetsBtn);
    $("#savedParamsSetsControls").append(addParamSetsLocBtn);

    $("#viewTab").tabs({
        active: 0,
        activate: function( event, ui )
        {
            test_editor.currentParamSetGroup = null;

            var index = ui.newTab.index();
            if(index == 1)
            {
                //view parameter sets
                loadAllParamSetGroups();
            }
            if(index == 2)
            {
                //view test results
                loadAllTestResults();
            }
        }
    });

    $("#settingsTab").tabs({
        active: 0
    });

    //add tooltips
    $("#viewParamSets").tooltip({
        show: {
            delay: 700
        }
    });

    $("#server").combobox(
    {
        change: function(event, ui)
        {
            $(".moduleVersionSelect").children("option").each(function(index, obj)
            {
                if(index != 0)
                {
                    $(this).remove();
                }
            });
            $(".moduleVersionSelect").multiselect("refresh");
            $(".updateModVersionStatus").hide();
        }
    });

    if(test_editor.server == "" || test_editor.username == "")
    {
        $("#loginDiv").show();
    }

    $("#loginButton").button().click(function()
    {
        test_editor.server = $("#server").val();
        test_editor.username = $("#username").val();

        if( test_editor.server != "" && test_editor.username != "")
        {
            $("#loginDiv").hide();
            $("#loginDiv").appendTo("body");
            $("#editRunSettings").hide();
            $("#editRunSettings").appendTo("#loginDiv");
            $("#createSettings").append("<p> Server: " + test_editor.server + "</p>");
            $("#createSettings").append("<p> Username: " + test_editor.username + "</p>");
            $("#title").prependTo(".center-west");
            displayMainPage();
        }
    });

    $(document).on("click", "#createTestButton", function()
    {
        var pNamesKey = Object.keys(pset_config);

        //Create parameter sets based on config
        var validCount = 0;
        var invalidCount = 0;

        var defaultTestCreated = false;

        var parameterSetItems = [];
        var defaultValues = {};
        var optionalParams = [];
        var alternateParamsInfo = {};
        for(var p=0;p<pNamesKey.length;p++)
        {
            var pName = pNamesKey[p];
            var paramConfig = pset_config[pName];

            //keep track of alternate names for pipelines
            var altName = getPropertyValueFromParamConfig(pName, "altName");
            if(altName != undefined && altName != null && altName.length > 0)
            {
                alternateParamsInfo[pName] = {};
                alternateParamsInfo[pName].name = altName;
            }
            //keep track if this parameter is optional
            var optional = getPropertyValueFromParamConfig(pName, "optional");
            if(optional)
            {
                optionalParams.push(pName);
            }
            var validValues = paramConfig["valid"];
            var isFixed = paramConfig["isFixed"];
            var isFile = paramConfig["isFile"];
            var isChoice = paramConfig["type"] == "choice";

            if((isFixed != undefined && isFixed) || (isFile != undefined && isFile && !isChoice) ||
                    validValues == null || validValues == undefined)
            {
                continue;
            }

            for(var v=0; v <validValues.length;v++)
            {
                var parameterSet = {};

                var parameters = [];
                //check if this is a test using default values
                var defaultTest = false;

                for(var s=0;s<pNamesKey.length;s++)
                {
                    var pConfig = pset_config[pNamesKey[s]];
                    var value = "";

                    //check if the value should be fixed for this param
                    var ispFile = pConfig["isFile"];
                    var ispFixed = pConfig["isFixed"];

                    if((ispFile != undefined && ispFile)
                        || (ispFixed != undefined && ispFixed))
                    {
                        var pvalidValues = pConfig["valid"];

                        if(pvalidValues != undefined && pvalidValues != null
                                && pvalidValues.length > 0)
                        {
                            value = pvalidValues[0];
                        }
                    }
                    else if(pName != pNamesKey[s])
                    {
                        if(pConfig["default"] != null && pConfig["default"] != undefined)
                        {
                            value = pConfig["default"];
                            defaultValues[pNamesKey[s]] = value;
                        }
                    }
                    else
                    {
                        value = validValues[v];

                        if(pConfig["default"] == value)
                        {
                            defaultTest = true;
                            defaultValues[pName] = value;                            
                        }
                    }

                    //test to prevent duplicate tests containing default values
                    if(defaultTestCreated && defaultTest)
                    {
                        break;
                    }

                    var validParamObj = {};
                    validParamObj["name"] = pNamesKey[s];
                    validParamObj["value"] = value;
                    parameters.push(validParamObj);                   
                }

                if(!defaultTestCreated || !defaultTest)
                {
                    validCount++;
                    var pSetNum = "valid_pset_" + validCount;
                    parameterSet["name"] = "Valid Parameter Set " + (validCount);

                    parameterSet["params"] = parameters;

                    //This is a valid test so it should not generate stderr
                    parameterSet["assertions"] =
                    {
                        jobStatus: "success"
                    };

                    parameterSet["id"] = pSetNum;
                    parameterSetItems.push(parameterSet);

                    if(defaultTest)
                    {
                        defaultTestCreated = true;
                    }
                }               
            }

            //do the same for invalid parameter sets
            var invalidValues = paramConfig["invalid"];

            if((isFile != undefined && isFile) ||
               invalidValues == null || invalidValues == undefined)
            {
                continue;
            }

            for(v=0; v <invalidValues.length;v++)
            {
                parameterSet = {};

                parameters = [];
                //check if this is a test using default values
                defaultTest = false;

                for(s=0;s<pNamesKey.length;s++)
                {
                    pConfig = pset_config[pNamesKey[s]];
                    value = "";

                    //check if the value should be fixed for this param
                    //check if the param is a file
                    //check if the value should be fixed for this param
                    var isAFile = pConfig["isFile"];
                    var isAFixed = pConfig["isFixed"];


                    if((isAFile != undefined && isAFile)
                        || (isAFixed != undefined && isAFixed))
                    {
                        pvalidValues = pConfig["valid"];

                        if(pvalidValues != undefined && pvalidValues != null
                                && pvalidValues.length > 0)
                        {
                            value = pvalidValues[0];
                        }
                    }
                    else if(pName != pNamesKey[s])
                    {
                        if(pConfig["default"] != null && pConfig["default"] != undefined)
                        {
                            value = pConfig["default"];
                            defaultValues[pNamesKey[s]] = value;
                        }
                    }
                    else
                    {
                        value = invalidValues[v];

                        if(pConfig["default"] == value)
                        {
                            defaultTest = true;
                            defaultValues[pName]= value;                            
                        }
                    }

                    if(defaultTestCreated && defaultTest)
                    {
                        break;
                    }

                    var invalidParamObj = {};
                    invalidParamObj["name"] = pNamesKey[s];
                    invalidParamObj["value"] = value;
                    parameters.push(invalidParamObj);
                }

                if(!defaultTestCreated || !defaultTest)
                {
                    invalidCount++;
                    pSetNum = "invalid_pset_" + invalidCount;
                    parameterSet["name"] = "Invalid Parameter Set " + (invalidCount);

                    parameterSet["params"] = parameters;

                    //This is a valid test so it should not generate stderr
                    parameterSet["assertions"] =
                    {
                        jobStatus: "error"
                    };

                    parameterSet["id"] = pSetNum;
                    parameterSetItems.push(parameterSet);

                    if(defaultTest)
                    {
                        defaultTestCreated = true;
                    }
                }
            }
        }

        var groupInfo = parameterSets["group_info"];
        if(groupInfo == undefined || groupInfo == null)
        {
            groupInfo = {};
            parameterSets["group_info"] = groupInfo;
        }
        //add name of module
        groupInfo["module"] = test_editor.module;
        groupInfo["lsid"] = test_editor.lsid;
        groupInfo["defaults"] = defaultValues;
        groupInfo["optional"] = optionalParams;
        groupInfo["altParamInfo"] = alternateParamsInfo;
        parameterSets["param_sets"] = parameterSetItems;

        viewParameterSets(parameterSets, false);
    });

    $(document).on("change", ".pSelector", function()
    {
        //remove meta data for custom and numeric values
        $(this).parents("tr:first").next(".typeMetadata").remove();
        $(this).siblings(".editCustomValues").remove();

        var value = $(this).val();

        //name of parameter being edited
        var pName = $(this).parents("tr:first").data("pname");

        var pNameWithSpace = pName.replace(/\./g,' ');

        var paramConfig = pset_config[pName];

        if(paramConfig == null)
        {
            paramConfig = {};
        }

        //get valid and invalid values for the parameter
        var validValues = getPropertyValueFromParamConfig(pName, "valid");
        var invalidValues = getPropertyValueFromParamConfig(pName, "invalid");

        addPropertyToParamConfig(pName, "valid", []);
        addPropertyToParamConfig(pName, "invalid", []);

        if(value == "SERVER_FILE")
        {
            if($(this).parents("tr:first").next().next(".serverFile").length > 0)
            {
                $(this).parents("tr:first").next().next(".serverFile").show();

                $(this).parents("tr:first").next(".choiceFile").hide();
            }
            else
            {
                $(this).parents("tr:first").next(".serverFile").show();
            }
        }
        else if(value == "SELECT_FILE")
        {
            $(this).parents("tr:first").next(".choiceFile").show();

            $(this).parents("tr:first").next().next(".serverFile").hide();
        }
        else if(value == "integer" || value == "float")
        {
            paramConfig["numType"] = value;

            var numSelector = $("<select>" +
                               "<option value='any'>Any</option>" +
                                "<option value='positive'>Positive</option>" +
                                "<option value='negative'>Negative</option>" +
                                "<option value='range'>Specify range</option>" +
                               "</select>");
            numSelector.change(function()
            {
                var value = $(this).val();

                var paramConfig = pset_config[pName];

                if(paramConfig == null)
                {
                    paramConfig = {};
                }

                var validValues = [];
                var invalidValues = [];

                var pNameWithoutDot = pName.replace(/\./g,'');               
                //remove range row
                $("#" + pNameWithoutDot + "Range").remove();
                
                if(value == "positive")
                {
                    if(paramConfig["numType"] == "integer")
                    {
                        validValues.push(getRandomInt(1, 100).toString());
                        validValues.push(getRandomInt(101, 1001).toString());
                    }
                    else
                    {
                        validValues.push(getRandomNum(1, 100).toString());
                        validValues.push(getRandomNum(101, 1001).toString());
                    }

                    invalidValues.push("stringValue");

                    invalidValues.push(getRandomNum(-100, -1).toString());
                }
                else if(value == "negative")
                {
                    if(paramConfig["numType"] == "integer")
                    {
                        validValues.push(getRandomInt(-100, 1).toString());
                        validValues.push(getRandomInt(-1001, -101).toString());
                    }
                    else
                    {
                        validValues.push(getRandomNum(-100, 1).toString());
                        validValues.push(getRandomNum(-1001, -101).toString());
                    }

                    invalidValues.push("stringValue");

                    invalidValues.push(getRandomNum(1, 100).toString());

                }
                else if(value == "range")
                {
                    var rangeRow = $("<tr id='" + pNameWithoutDot +"Range'/>");
                    //start in second column to align with value column in table
                    rangeRow.append("<td/>");
                    var rangeValueTd = $("<td colspan='0'/>");

                    var rangeSelector = $("<select class='rangeSelector'>" +
                               "<option value='greater'>&gt;</option>" +
                               "<option value='greaterEqual'>&gt;=</option>" +
                               "</select>");

                    rangeValueTd.append(rangeSelector);

                    rangeSelector.multiselect({
                        /*position: {
                            my: 'left bottom',
                            at: 'left top'
                        },*/
                        multiple: false,
                        minWidth: 64,
                        selectedList: 1
                    });

                    var minRangeInput = $("<input type='text' class='rangeValue'/>").change(function()
                    {
                        if($(this).val() == null || $(this).val() == "")
                        {
                            return;
                        }

                        var paramConfig = pset_config[pName];

                        if(paramConfig == null)
                        {
                            paramConfig = {};
                        }

                        var validValues = [];
                        var invalidValues = [];

                        var maxValue = $(this).parents("tr:first").data("maxRangeValue");
                        var minValue = $(this).val();

                        if(paramConfig["numType"] == "integer")
                        {
                            minValue = parseInt(minValue);
                        }

                        if(paramConfig["numType"] == "float")
                        {
                            minValue = parseFloat(minValue);
                        }

                        if(isNaN(minValue))
                        {
                            createErrorMsg("Invalid Range", "Minimum range value must be a number");
                            $(this).val("");
                            return;
                        }

                        if(maxValue != null && maxValue != "")
                        {
                            if(maxValue == minValue)
                            {
                                createErrorMsg("Invalid Range", "Minimum range value cannot be the same as the maximum range value");
                                $(this).val("");
                                return;
                            }

                            if(minValue > maxValue)
                            {
                                createErrorMsg("Invalid Range", "Minimum range value cannot be greater than the max range value");
                                $(this).val("");
                                return;
                            }
                        }
                        else
                        {
                            maxValue = minValue + 1000;
                        }

                        var middle = maxValue - minValue;
                        middle = middle / 2;
                        if(paramConfig["numType"] == "integer")
                        {
                            validValues.push(getRandomInt(minValue, middle).toString());
                            validValues.push(getRandomInt(middle+1, maxValue).toString());
                        }
                        else
                        {
                             validValues.push(getRandomNum(minValue, middle).toString());
                             validValues.push(getRandomNum(middle+1, maxValue).toString());
                        }

                        invalidValues.push(getRandomInt(minValue-1, minValue-100).toString());
                        invalidValues.push(getRandomNum(maxValue+1, maxValue+100).toString());
                        invalidValues.push("stringValue");

                        $(this).parents("tr:first").data("minRangeValue", minValue);
                    });

                    rangeValueTd.append(minRangeInput);
                    rangeValueTd.append("<span class='andText'>and</span>");

                    rangeSelector = $("<select>" +
                                "<option value='less'>&lt;</option>" +
                                "<option value='lessEqual'>&lt;=</option>" +
                               "</select>");
                    rangeValueTd.append(rangeSelector);
                    rangeSelector.multiselect({
                        minWidth: 64,
                        multiple: false,
                        selectedList: 1
                    });

                    var maxRangeInput = $("<input type='text' class='rangeValue'/>").change(function()
                    {
                        if($(this).val() == null || $(this).val() == "")
                        {
                            return;
                        }

                        var validValues = [];
                        var invalidValues = [];

                        var minValue = $(this).parents("tr:first").data("minRangeValue");
                        var maxValue = $(this).val();

                        if(paramConfig["numType"] == "integer")
                        {
                            maxValue = parseInt(maxValue);
                        }

                        if(paramConfig["numType"] == "float")
                        {
                            maxValue = parseFloat(maxValue);
                        }

                        if(isNaN(maxValue))
                        {
                            createErrorMsg("Invalid Range", "Maximum range value must be a number");
                            $(this).val("");
                            return;
                        }

                        if(maxValue != null && maxValue != "")
                        {
                            if(maxValue == minValue)
                            {
                                createErrorMsg("Invalid Range", "Maximum range value cannot be the same as the minimum range value");
                                $(this).val("");
                                return;
                            }

                            if(maxValue < minValue)
                            {
                                createErrorMsg("Invalid Range", "Maximum range value cannot be less than the minimum range value");
                                $(this).val("");
                                return;
                            }
                        }
                        else
                        {
                            minValue = maxValue - 1000;
                        }

                        var middleValue = maxValue - minValue;

                        if(paramConfig["numType"] == "integer")
                        {
                            middleValue = minValue + Math.floor(middleValue / 2);

                            validValues.push(getRandomInt(minValue, middleValue).toString());
                            validValues.push(getRandomInt(middleValue+1, maxValue).toString());
                        }
                        else
                        {
                            middleValue = minValue + (middleValue / 2);
                            validValues.push(getRandomNum(minValue, middleValue).toString());
                            validValues.push(getRandomNum(middleValue+1, maxValue).toString());
                        }

                        invalidValues.push(getRandomInt(minValue-100, minValue-1).toString());
                        invalidValues.push(getRandomNum(maxValue+1, maxValue+100).toString());
                        invalidValues.push("stringValue");

                        addPropertyToParamConfig(pName, "valid", validValues);
                        addPropertyToParamConfig(pName, "invalid", invalidValues);

                        $(this).parents("tr:first").data("maxRangeValue", maxValue);
                    });

                    rangeValueTd.append(maxRangeInput);

                    rangeRow.append(rangeValueTd);
                    $(this).parents("tr:first").after(rangeRow);
                }
                else
                {
                    //than option selected must be any numerical value
                    validValues.push(getRandomInt(-100, 1).toString());
                    validValues.push(getRandomNum(101, 1001).toString());

                    invalidValues.push("stringValue");                    
                }

                addPropertyToParamConfig(pName, "valid", validValues);
                addPropertyToParamConfig(pName, "invalid", invalidValues);
            });

            var numSelectRow = $("<tr class='typeMetadata'/>");
            var numSelectData = $("<td/>");
            numSelectData.append(numSelector);
            //start in second column
            numSelectRow.append("<td/>");
            numSelectRow.append(numSelectData);
            $(this).parents("tr:first").after(numSelectRow);

            //$(this).parent().append(numSelector);
            numSelector.multiselect({
                /*position: {
                    my: 'left bottom',
                    at: 'left top'
                },*/
                minWidth: 148,                
                multiple: false,
                selectedList: 1
            });
        }

        else if(value == "custom")
        {
            var selectedRow = $(this).parents("tr:first");
            //highlight the row containing the parameter that is being edited
            selectedRow.find("td").addClass("selected");

            var editButton = $("<button class='editCustomValues'>Edit</button>");
            editButton.button().click(function()
            {
                $(this).siblings(".pSelector").trigger("change");    
            });

            $(this).parent().append(editButton);
            var valuesDialog = $("<div/>");
            valuesDialog.append("<p>Parameter name: " + pNameWithSpace + "</p>");
            var addButton = $("<button>Add</button>");
            addButton.button().click(function()
            {
                $("#validValuesTable").append("<tr><td><input type='text' name='validValue'/></td></tr>");
                $("#invalidValuesTable").append("<tr><td><input type='text' name='invalidValue'/></td></tr>");                
            });

            var bDiv = $("<div/>");
            bDiv.append(addButton);
            valuesDialog.append(bDiv);

            var minRows = 2;
            if(validValues != null && minRows < validValues.length)
            {
                minRows = validValues.length;
            }

            if(invalidValues != null && minRows < invalidValues.length)
            {
                minRows = invalidValues.length;
            }

            var validValuesTable = $("<table id='validValuesTable'></table>");
            validValuesTable.append("<tr><th>Valid Values</th></tr>");

            var invalidValuesTable = $("<table id='invalidValuesTable'></table>");
                        invalidValuesTable.append("<tr><th>Invalid Values</th></tr>");

            var numRows = 0;

            if(validValues != null && validValues.length > 0)
            {
                for(var v=0;v<validValues.length;v++)
                {
                    validValuesTable.append("<tr><td><input type='text' name='validValue'" +
                                          " value='"+ htmlEncode(validValues[v])+ "'/></td></tr>");
                    numRows++;
                }
            }

            if(validValues == null || validValues.length < minRows)
            {
                //initialize with two empty fields
                for(y=0;y<(minRows-numRows);y++)
                {
                    validValuesTable.append("<tr><td><input type='text' name='validValue'/></td></tr>");
                }
            }

            numRows = 0;
            if(invalidValues != null && invalidValues.length > 0)
            {
                for(v=0;v<invalidValues.length;v++)
                {
                    invalidValuesTable.append("<tr><td><input type='text' name='invalidValue'" +
                                          " value='"+ invalidValues[v]+ "'/></td></tr>");
                    numRows++;
                }
            }

            if(invalidValues == null || invalidValues.length < minRows)
            {
                //initialize with two empty fields
                for(var y=0;y<(minRows-numRows);y++)
                {
                    invalidValuesTable.append("<tr><td><input type='text' name='invalidValue'/></td></tr>");
                }
            }
            

            valuesDialog.append(validValuesTable);
            valuesDialog.append(invalidValuesTable);

            valuesDialog.data("pname", pName);
            valuesDialog.dialog({
                autoOpen: true,
                height: 520,
                width: 670,
                title: "Specify Valid/Invalid values",
                buttons: {
                    "OK": function() {
                        var pName = $(this).data("pname");
                        var paramConfig = pset_config[pName];

                        if(paramConfig == null)
                        {
                            paramConfig = {};
                        }

                        var validValues = [];
                        $("#validValuesTable").find("input[name='validValue']").each(function()
                        {
                            if($(this).val() != "")
                            {
                                validValues.push($(this).val());
                            }
                        });

                        var invalidValues = [];
                        $("#invalidValuesTable").find("input[name='invalidValue']").each(function()
                        {
                            if($(this).val() != "")
                            {
                                invalidValues.push($(this).val());
                            }
                        });

                        paramConfig["valid"] = validValues;
                        paramConfig["invalid"] = invalidValues;

                        pset_config[pName] = paramConfig;

                        $( this ).dialog( "destroy" );
                        $(this).remove();
                        selectedRow.find("td").removeClass("selected");
                    },
                    "Cancel": function() {
                        $( this ).dialog( "destroy" );
                        $(this).remove();
                        selectedRow.find("td").removeClass("selected");
                    }
                },
                resizable: true
            });
        }
        else
        {
            //this must be a choice drop down value so add
            // it to valid value list for the parameter
            var selectedValues = $(this).multiselect("getChecked");
            
            validValues = [];
            if(selectedValues.length > 0)
            {
                for(var s=0;s<selectedValues.length;s++)
                {
                    validValues.push(selectedValues[s].value);
                }
            }

            addPropertyToParamConfig(pName, "valid", validValues);
        }
    });

    $(document).on("change", ".typeMetadata", function()
    {
        var value = $(this).val();

        /*if(value == "positive" || "negative" || "range")
        {
            var numValid = "<label for='numValid'>Num. valid </label>" +
                            "<input type='text' name='numValid'/>"
            $(this).parent().append(numValid);
        } */
    });

    $("button").button();

    $("#refreshTasks").click(function()
    {
        loadAllTasks();    
    });

    $("#refreshTests").click(function()
    {
        loadAllTestResults();
    });


    $(".moduleVersionSelect").multiselect(
    {
        header: false,
        multiple: false,
        selectedList: 1,
        close: function()
        {
            $(".updateModVersionStatus").hide();
        }
    });

    $(".updateModVersionStatus").hide();

    $(".updateModVersion").click(function()
    {
        if($("#server").val() == undefined || $("#server").val() == null || $("#server").val().length < 1)
        {
            createErrorMsg("Server Login", "Please enter a server name");
        }

        if($("#username").val() == undefined || $("#username").val() == null || $("#username").val().length < 1)
        {
            createErrorMsg("Server Login", "Please enter a user name");
        }

        $.ajax({
            type: "POST",
            url: window.location.pathname + "AutomatedTest/getModuleVersions",
            data: { "server" : $("#server").val(),
                "username" : $("#username").val(),
                "password": $("#password").val(),
                "lsid": test_editor.lsid},
            success: function(response) {
                var message = response["MESSAGE"];
                var error = response["ERROR"];

                if (error !== undefined && error !== null)
                {
                    createErrorMsg("Module Versions", htmlEncode(error));
                    return;
                }
                if (message !== undefined && message !== null) {
                    createInfoMsg("Module Versions", htmlEncode(message));
                    return;
                }

                updateModuleVersions(response["lsidVersions"]);
                $(".updateModVersionStatus").show();
            },
            error: function (xhr, ajaxOptions, thrownError) {
                createErrorMsg("Module Versions", "Error status: " + xhr.status);

                if(thrownError != null && thrownError != undefined &&thrownError != "")
                {
                    createErrorMsg("Module Versions", htmlEncode(thrownError));
                }
            },
            dataType: "json"
        });
    });

    mainLayout = $('body').layout({
        center__paneSelector:	".outer-center"
    ,    west__paneSelector:	".outer-west"
    //	enable showOverflow on west-pane so CSS popups will overlap north pane
    ,    west__showOverflowOnHover: false

    //	reference only - these options are NOT required because 'true' is the default
    ,	closable:				true
    ,	resizable:				true	// when open, pane can be resized
    ,	slidable:				false	// when closed, pane can 'slide' open over other panes - closes on mouse-out

    //	some resizing/toggling settings
    ,	north__slidable:		false	// OVERRIDE the pane-default of 'slidable=true'
    ,	north__spacing_open:	0		// no resizer-bar when open (zero height)
    ,	north__spacing_closed:	30		// big resizer-bar when open (zero height)
    ,   north__initHidden:      true
    ,	south__spacing_open:	0

    ,	south__slidable:		false	// OVERRIDE the pane-default of 'slidable=true'
        //some pane-size settings
    ,	north__minHeight:		260
    ,	north__height:		    260
    ,	south__minHeight:		60
    ,	west__size:			    360
    ,	west__minWidth:			320
    ,   west__initHidden:       true
    ,	east__size:				300
    ,   center__minWidth:       100
    ,	useStateCookie:			true
    });

    centerInnerLayout = $('.outer-center').layout({
        //	enable showOverflow on west-pane so CSS popups will overlap north pane
            west__showOverflowOnHover: true

        //	reference only - these options are NOT required because 'true' is the default
        ,	closable:				true
        ,	resizable:				true	// when open, pane can be resized
        ,	slidable:				false    // when closed, pane can 'slide' open over other panes - closes on mouse-out
        ,   center__paneSelector:	".middle-center"
        ,   center__minWidth:       100
        ,	north__paneSelector:    ".middle-north"
        ,	south__paneSelector:	".middle-south"
        //,	north__size:			50
        ,	north__minHeight:	    60
        ,	north__spacing_open:	0		// no resizer-bar when open (zero height)
        ,	north__spacing_closed:	0		// big resizer-bar when open (zero height)        
        ,   north__initHidden:      true
        ,	south__minHeight:	    120
        ,	south__spacing_open:	0		// no resizer-bar when open (zero height)        
        ,   south__initHidden:      true
    });

    westInnerLayout = $('.outer-west').layout({
        //	enable showOverflow on west-pane so CSS popups will overlap north pane
        //    west__showOverflowOnHover: false
        //	reference only - these options are NOT required because 'true' is the default
        	closable:				true
        ,	resizable:				true	// when open, pane can be resized
        ,	slidable:			    false    // when closed, pane can 'slide' open over other panes - closes on mouse-out
        ,   center__paneSelector:	".center-west"
        ,   center__minWidth:       420
        ,   center__width:       420
        ,	south__paneSelector:	".south-west"
        ,	south__minHeight:	    60
    });


    mainLayout.close('west');

});
