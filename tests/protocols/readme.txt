#
# The protocols test suite
#

A gpunit test suite which runs all of the example jobs from the protocols page of the GenePattern server.

To add a new test to the suite:
    1) create a new directory, called the test directory.
    2) create a new file, e.g. 'test.yaml', called the test file.
    3) input files must be declared by external url, or by relative path to the test.
    4) declare assertions. The simplest test case involves creating an 'expected' folder,
       name must be an exact match, into the same directory as the test. 
       When the job completes, gp-unit will do a diff comparison between the contents of the 'expected' folder,
       and the jobResults folder. If any differences are found, the test will fail.

By convention, all files which match the pattern, *test.yaml or *test.yml, will be run as gpunit tests.
