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
locally. It uses well established technologies: junit and ant. 
The idea is to use as much of these existing technologies 
as possible, implementing new functionality only when needed.

--------------------
Requirements
--------------------
* Requires ant-1.8
* Requires a newer version of junit. This project includes
  a snapshot build of junit-4.11. You must use this version
  or later, because of the dependency on the Parameterized test class.
* a running GP server. You can connect to any GP server, including gpprod
  for running the tests.

--------------------
Using Gpunit
--------------------
The easiest way to get started is to get the latest version of gpunit from SVN:
    svn export https://svn.broadinstitute.org/gp2/trunk/modules/util/gp-unit 
Edit the configuration file as necessary.
    gpunit.properties
Then run the ant 'gpunit' target.
    ant gpunit

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
non-default parameter values, and expected outputs in the form of gpunit assertions.
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
There are a number of ways to customize gpunit. Start by looking at the
comments in the gpunit.properties file.

--------------------
Gpunit Yaml Formal
--------------------
A gpunit test case is declared in a YAML file. While there is no schema
for a gpunit yaml file (it is schemaless), there is an implicit schema.
There are a number of example gpunit test cases in this project.

At the moment, if you need more details you will have to look at the
example gpunit files included in this project. For more details, look 
at the source code.

 Yaml is a standardized 
format. The structure is a schemaless custom stucture required by the gpunit system.

A gpunit test case is declared in a test case yaml file.
Note: The automatrix is a gui for automatically generating 
these files.

You can also easily convert a completed job on a GP server into a gpunit
test case.

--------------------
How it works
--------------------
A gpunit test run is invoked with the ant 'junit' target, hard-coded to a single test:
    org.genepattern.gpunit.test.BatchModuleTest
The 'gpunit.testcase.dirs' is an ant FileSet. Each matching file in the fileset is
treated as a single gpunit test. For more details look at the 'run-tests' macrodef
in the build.xml file.

    <junit ...>
        <test name="org.genepattern.gpunit.test.BatchModuleTest" ... />
    </junit>

From the perspective of junit, BatchModuleTest is a Parameterized test which
has been extended to run each test in parallel. It uses the 'gpunit.testcase.dirs'
system property to scan the file system for gpunit test case files.



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

--------------------
About Test Execution
--------------------

--------------------
Developing Gpunit
--------------------
For more information, see the comments in the java source code.
Start with the org.genepattern.gpunit.test.BatchModuleTest.java class.




Overview

A gpunit test case 



* How to get started
** building gp-unit
** deploying gp-unit
** running gp-unit tests


* How to create a unit test
* How to choose a server
* Note: REST vs. SOAP client

TODO:
* label the gp-unit jar
* improve properties for username, password, and gp server
** by convention, look in a well named properties file, otherwise prompt
