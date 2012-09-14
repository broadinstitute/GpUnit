 These test-cases demonstrate different ways to customize the diff command which is used to validate job results files.
 
To declare a custom diff algorithm for a test case, add an enty to the assertions section.
Option 1: Applies to all defined result files:
assertions:
    diffCmd: diff -q
    files:
        example_diff_input.cvt.txt:
            diff: ./expected.files/all_aml_test.preprocessed.sub28.2.clu

Option 2: Applies on a per-file basis
assertions:
    files:
        example_diff_input.cvt.txt:
            diffCmd: diff -q
            diff: ./expected.files/all_aml_test.preprocessed.sub28.2.clu

There are several ways to define the custom diff command. They break down to two options,
declare a command line diff script, or (b) implement a new java class as a subclass of AbstractDiffTest.

assertions:
    # it's an executable (script) saved in the same directory as this test-case file
    diffCmd: ./myDiff.sh
    # can be a relative path to this test-case file
    diffCmd: ../copyOfmyDiff.sh
    # can be an executable, must be on the exec PATH
    diffCmd: diff -q
    # can be a fully qualified path to an executable
    diffCmd: /usr/bin/diff
    # can be a different java class which extends the AbstractDiffTest class
    diffCmd: org.genepattern.gpunit.diff.UnixCmdLineDiff
    # the 'org.genepattern.gpunit.diff.CmdLineDiff' is optional, but it works
    diffCmd: org.genepattern.gpunit.diff.CmdLineDiff diff -q
