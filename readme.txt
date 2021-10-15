**ARCHIVED - active repository now in the GenePattern GitHub org - https://github.com/genepattern/GPUnit **

GpUnit: A testing framework for GenePattern Modules.

--------------------
About GpUnit
--------------------
GpUnit is a testing framework for GenePattern Modules. The framework
consists of:
    1) a yaml file format for declaring test cases
    2) an ant build file for executing test cases as JUnit tests
    3) a convention for declaring a suite as a collection of test cases

1) Test cases are declarative. This gives us a platform agnostic way to
document expected inputs and outputs for a given module.

2) Test cases are run as JUnit tests. Consequently, you can do anything 
with a GpUnit test that you can do with a JUnit test, including:
    a) run GpUnit tests from an IDE such as Eclipse or IntelliJ
    b) run GpUnit tests from the command line with ant and the junit target.
    c) run a suite of GpUnit tests on demand, as part of a regression test.
    d) run a suite of GpUnit tests automatically, as part of a continuous integration system.

3) A suite is declared as an ant FileSet. Consequently, if you know what
a FileSet is, and how to declare one, you know how to declare a
GpUnit suite.

A test consists of a run of a module on a GP server, followed by a validation step run
locally.

--------------------
Requirements
--------------------
* Requires ant-1.8
* Requires a newer version of JUnit. This project includes
  a snapshot build of junit-4.11. You must use this version
  or later, because of the dependency on the Parameterized test class.
* a running GP server. You can connect to any GP server, including gpprod
  for running the tests.
* GP Java client. When you run GpUnit as an ant target, it will automatically
  download the Java client library from the server before running the tests.

--------------------
Using GpUnit
--------------------
The easiest way to get started is to get the latest version of GpUnit from GitHub:
    https://github.com/broadinstitute/GpUnit
Edit the configuration file as necessary.
    gpunit.properties
    gpunit.default.properties
Run the ant 'gpunit' target.
    ant gpunit
View the JUnit report.
    open reports/current/html/index.html

This target runs the default suite of GpUnit tests on the default 
GenePattern Server using the default username and password.
The output of this target is a JUnit report.

To use non-default values (which you should), you must customize
GpUnit. You do so in the same way that you customize an ant job,
with command line arguments and/or properties files. For more
details look at the 'gpunit.properties' file.

The recipe below gives more details.

Step 1: Declare test case(s)
In the abstract, a GpUnit test case consists of a module to run, a list of zero or more
non-default parameter values, and an assertion section which can include expected 
job status and expected outputs.
There are several ways to declare your test case:
1) hand edit a yaml file
2) export a completed job, including the gp_execution_log.txt, from a server
3) use Automatrix to generate the yaml file
Any input parameters specified in the yaml file can reference named properties
by using "<%prop_name%> delimiters. The property name must consist of alphabetic
or numeric characters, plus "." and "_"). The value of the property (which must
be defined in a properties file or on the Ant command line) will be expanded
by GpUnit before processing the test. In order to prevent GpUnit from treating
a name enclosed in "<%...%>" as a property to be expanded, you can use the escape
character "\" before the opening "<". Using an escape is not necessary if you simply
want to use a "<" or "<%" sequence in the yaml file, since GpUnit only does
the property expansion if the full pattern "<%prop.name%>" is matched.

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
this level of customization. Also see https://confluence.broadinstitute.org/pages/viewpage.action?pageId=67403904 for information on how to integrate with Continuous Integration. (needs Broad credentials to view)

Step 3: Run your test(s)
You run a test or suite of tests with the ant gpunit target.
    ant gpunit

--------------------
Customizing GpUnit
--------------------
There are a number of ways to customize GpUnit. For details,
look at the comments in the gpunit.properties file.

--------------------
GpUnit Yaml Format
--------------------
A GpUnit test case is declared in a YAML file. While there is no schema
for a GpUnit yaml file (it is schemaless), there is an implicit schema.
At the moment, if you need more details you will have to look at the
example GpUnit files included in this project. For even more details, look 
at the source code.

--------------------
Common dataset locations
--------------------
ftp://ftp.broadinstitute.org/pub/genepattern/datasets/
http://www.broadinstitute.org/cancer/software/genepattern/data/all_aml/all_aml_train.gct

ftp://gpftp.broadinstitute.org/pub/genepattern/datasets/protocols/


--------------------
Converting a completed job into a test
--------------------
You can also easily convert a completed job on a GP server into a GpUnit
test case. Look at the ./tests/saved_jobs folder for some examples.

A few manual steps are required, especially if you uploaded data
files for the initial run of the job.
External URLs work automatically because they are passed by 
reference when you download the job.

--------------------
Building GpUnit
--------------------
The ant 'gpunit' target will build GpUnit when necessary. In most cases
that is all you need to do.

If you want to build the GpUnit jar file first run this command:
    ant package

Both the 'gpunit' and 'package' targets require the GenePattern Java Client.
This is required for the 'SOAP' client callbacks which are built into
GpUnit. This gets installed for you automatically.

--------------------
Initializing GP Java Client
--------------------
GpUnit runs jobs on a (usually) remote GenePattern Server. By default,
it makes REST client calls to a running GenePattern server. The REST
API was added to GenePattern with the 3.6.0 release.

You can switch to the SOAP client with the 'gpunit.client=SOAP' configuration option.
With the SOAP client, when you run the gpunit target from ant, if necessary it will 
automatically download the Java SOAP client before building GpUnit. 
For more details look at the build.xml file.

--------------------
Testing HTTPS server
--------------------
You may need to take some extra steps when running GpUnit tests against an HTTPS server. If you see an 
SSLHandshakeException you may need to add the certificate to a keystore and configure your GpUnit tests
to use it. Example error message:

    javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: \
        sun.security.provider.certpath.SunCertPathBuilderException: \
        unable to find valid certification path to requested target

(1) Download the certificate file. From a web browser, open a link to the server, 
and save the certificate file, e.g. 'gpserver.cert'.

(2) Add the certificate to a new or existing keystore file,  e.g. 'gpunit_keystore.jks'
    keytool -import -file gpserver.cert -keystore gpunit_keystore.jks

(3) Set 'gpunit.trustStore' and 'gpunit.trustStorePassword' properties.
    gpunit.trustStore=gpunit_keystore.jks
    gpunit.trustStorePassword=my_password

These are passed along as java command line args by the ant <junit> task.
    -Djavax.net.ssl.trustStore=${gpunit.trustStore} 
    -Djavax.net.ssl.trustStorePassword=${gpunit.trustStorePassword}
The tests make REST API calls to the server.

(4) Optionally skip the 'check-url' target. Before running the tests, GpUnit checks the connection with a curl command. 
To by-pass this test:
    -Dcheck-url.skip=1

(5) Use Java 8+ if you can. I was not able to connect to some HTTPS servers when running gpUnit with Java 7.
For best results use Java 8+. This thread describes additional JSSE Tuning Parameters which:
    https://blogs.oracle.com/java-platform-group/entry/diagnosing_tls_ssl_and_https
Example,
    -Djavax.net.debug=ssl, for debugging
    -Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2

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
A JUnit test is run in a new JVM, "org.genepattern.gpunit.test.BatchModuleTest".
A list of directories and or files are set by 'gpunit.testcase.dirs' system property.
With the help of the JUnit Parameterized class each matching GpUnit yaml file is
run as an individual JUnit test.

For each individual test case, the BatchModuleTest has code for:
    1) parsing the YAML file
    2) running a job on the GP server (via SOAP or possibly REST API calls)
    3) validating the results

The validation step uses standard JUnit Assertions to indicate pass or fail
status.

When all of the tests have completed or after a timeout period,
a JUnit report is generated, using standard ant mechanisms.

The GpUnit ant script runs on your client machine, the modules themselves
run on a gp server, and the validation process runs on your client machine.
If necessary, result files are downloaded to your client machine as
part of the validation process.

For more information, see the comments in the java source code.
Start with the org.genepattern.gpunit.test.BatchModuleTest.java class.
