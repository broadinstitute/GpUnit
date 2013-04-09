Gpunit: A testing framework for GenePattern Modules.

--------------------
About Gpunit
--------------------
Gpunit is a testing framework for GenePattern Modules. The framework
consists of:
    1) a method for declaring test cases
    2) a script for executing test cases
    3) a convention for declaring a suite as a collection of test cases

1) Test cases are declarative. This gives us a platform agnostic way to
document expected inputs and outputs for a given module.

2) Test cases are run as junit tests. Consequently, you can do anything 
with a gpunit test that you can do with a junit test, including:
    a) run gpunit tests from an IDE such as Eclipse or IntelliJ
    b) run gpunit tests from the command line with ant and the junit target.
    c) run a suite of gpunit tests on demand, as part of a regression test.
    d) run a suite of gpunit tests automatically, as part of a continuous integration system.

3) A suite is declared as an ant FileSet. Consequently, if you know what
a FileSet is, and how to declare one, you know how to declare a
gpunit suite.

A test consists of a run of a module on a GP server, followed by a validation step run
locally.

--------------------
Requirements
--------------------
* Requires ant-1.8
* Requires a newer version of junit. This project includes
  a snapshot build of junit-4.11. You must use this version
  or later, because of the dependency on the Parameterized test class.
* a running GP server. You can connect to any GP server, including gpprod
  for running the tests.
* GP Java client. When you run gpunit as an ant target, it will automatically
  download the Java client library from the server before running the tests.

--------------------
Using Gpunit
--------------------
The easiest way to get started is to get the latest version of gpunit from SVN:
    svn export https://svn.broadinstitute.org/gp2/trunk/modules/util/gp-unit 
Edit the configuration file as necessary.
    gpunit.properties
Then run the ant 'gpunit' target.
    ant gpunit
Then view the report
    open reports/html/index.html

This target runs the default suite of gpunit tests on the default 
GenePattern Server using the deault username and password.
The output of this target is a junit report.

To use non-default values (which you should), you must customize
gpunit. You do so in the same way that you customize an ant job,
with command line arguments and/or properties files. For more
details look at the 'gpunit.properties' file.

The recipe below gives more details.

Step 1: Declare test case(s)
In the abstract, a gpunit test case consists of a module to run, a list of zero or more
non-default parameter values, and an assertion section which can include expected 
job status and expected outputs.
There are several ways to declare your test case:
1) hand edit a yaml file
2) export a completed job, including the gp_execution_log.txt, from a server
3) use Automatrix to generate the yaml file

Step 2: Declare your test suite
A test suite is literally an ant FileSet. The easiest way to declare your test suite
is with the 'gpunit.testcase.dirs' property, which can optionally be passed in as
a command line arg or in a property file.

To run a single test,
    gpunit.testcase.dirs=/tests/protocols/01_Run/01_PreprocessDataset/test.yaml
To run a suite of all matching tests in the 'protocols' directory
    gpunit.testcase.dirs=./tests/protocols
For more sophisticated patterns, consult the ant documentation of the
FileSet task. You will need to update the build.xml in order to use
this level of customization.

Step 3: Run your test(s)
You run a test or suite of tests with the ant gpunit target.
    ant gpunit

--------------------
Customizing Gpunit
--------------------
There are a number of ways to customize gpunit. For details,
look at the comments in the gpunit.properties file.

--------------------
Gpunit Yaml Format
--------------------
A gpunit test case is declared in a YAML file. While there is no schema
for a gpunit yaml file (it is schemaless), there is an implicit schema.
At the moment, if you need more details you will have to look at the
example gpunit files included in this project. For even more details, look 
at the source code.

--------------------
Converting a completed job into a test
--------------------
You can also easily convert a completed job on a GP server into a gpunit
test case. Look at the ./tests/saved_jobs folder for some examples.

A few manual steps are required, especially if you uploaded data
files for the initial run of the job.
External URLs work automatically because they are passed by 
reference when you download the job.

--------------------
Building Gpunit
--------------------
The ant 'gpunit' target will build gpunit when necessary. In most cases
that is all you need to do.

If you want to build the gpunit jar file first run this command:
    ant package

Both the 'gpunit' and 'package' targets require the GP Java Client.
Normally, this gets installed for you automatically.
See below for more details.

--------------------
Initializing GP Java Client
--------------------
Gpunit runs jobs on a (usually) remote GenePattern Server. By default,
gpunit uses SOAP client calls for interacting with the GP server. When you 
run the gpunit target from ant, if necessary it will automatically download 
the Java client before building gpunit. For more details look at the build.xml file.

There is also a REST API for connecting to the GP server (circa 3.6.0). In
it's present incarnation, REST API methods are coded directly into the
gpunit project.

--------------------
How it works
--------------------
The build.xml file included in this project includes the 'gpunit' ant target.
This target uses the standard 'junit' ant target.
    <junit ...>
        <test name="org.genepattern.gpunit.test.BatchModuleTest" ... />
    </junit>
For more details look at the 'run-tests' macrodef.

Here is the what happens when you invoke the 'gpunit' target.
A junit test is run in a new JVM, "org.genepattern.gpunit.test.BatchModuleTest".
A list of directories and or files are set by 'gpunit.testcase.dirs' system property.
With the help of the junit Parameterized class each matching gpunit yaml file is
run as an individual junit test.

For each individual test case, the BatchModuleTest has code for:
    1) parsing the YAML file
    2) running a job on the GP server (via SOAP or possibly REST API calls)
    3) validating the results

The validation step uses standard junit Assertions to indicate pass or fail
status.

When all of the tests have completed or after a timeout period,
a junit report is generated, using standard ant mechanisms.

The gpunit ant script runs on your client machine, the modules themselves
run on a gp server, and the validation process runs on your client machine.
If necessary, result files are downloaded to your client machine as
part of the validation process.

For more information, see the comments in the java source code.
Start with the org.genepattern.gpunit.test.BatchModuleTest.java class.


TODO:
* label the gp-unit jar
* improve properties for username, password, and gp server
** by convention, look in a well named properties file, otherwise prompt the user
