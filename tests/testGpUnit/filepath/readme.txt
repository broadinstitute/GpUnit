Test cases for input file paths.

Given,
    name, the input parameter name
    value, input parameter value
    input parameter type, <type>
    [optional] gpunit.upload.dir
    [optional] gpunit.server.dir
    [optional] gpunit.diff.dir
    
Name and value are declared in the params section of a test.yaml file. For example,
params:
    input.filename: "input.txt"
    
Type is declared in the manifest file. For example,
p1_type=java.io.File

The gpunit.* properties are declared in the gpunit.properties file, or in the ant build.xml file.
gpunit.upload.dir, when this is set, look in this directory for input files to upload to the server.
gpunit.server.dir, when this is set, append this to the value for input files. Assume it's a server file path.
gpunit.diff.dir, when this is set, look in this directory when diff'ing job result files.
For example,
    gpunit.upload.dir=/Data/gpunit/input
    gpunit.server.dir=/xchip/sqa/TestFiles
    gpunit.diff.dir=/Data/gpunit/expected_output

When type equals "java.io.File", here are the rules for how gp-unit sets the value
for the input parameter.

(Note: these rules are implemented via the SOAP client, however, they should work similarly
    for the REST API, which is presently under development).
    
Note: "UPLOAD" means the file is uploaded from the local machine to the GP server for each run of the test.
Note: "LITERAL" means the file is  not uploaded, instead a literal value is passed to the GP server.
    It's up the the server to figure out how to convert this into a valid command line value for the module.
    Examples include:
        external URL
        server file path
        ERROR!, file not found
    
1) If the value is a URL ... always LITERAL
a) <GenePatternURL> substitution param
b) external URL
c) GP URL, not recommended, because the test is not portable

2) If value is a fully qualified path ...
a) if the file exists locally, UPLOAD
b) else LITERAL

3) Else value is a relative path ...
a) if the file exists relative to the directory which contains the test file, UPLOAD
b) else if gpunit.upload.dir is defined, if the file exists relative to gpunit.upload.dir, UPLOAD
   Note: if gpunit.upload.dir is relative, it means it is relative to the current working directory
c) else if gpunit.server.dir is defined, prepend gpunit.server.dir to the value. LITERAL.
d) else LITERAL

When diffing job results.
1) If a directory named 'expected' exists relative to the test directory ...
    Do a directory diff of all result files with the 'expected' directory.

2) Else if 'outputDir' is declared as one of the assertions,
a) if the output dir exists, do a directory diff with the output dir
b) else if gpunit.diff.dir is defined, prepend gpunit.diff.dir to the value 
    and do a directory diff

3) For individual files, listed in the assertions ...
a) if the file exists, diff the result file, with the file relative to the test file.
b) else if gpunit.diff.dir is defined, prepend gpunit.diff.dir to the value,
    and then do a file diff.
