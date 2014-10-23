These tests were developed to establish baseline performance metrics for the TopHat module. 
These were initially tested on a GP server running TopHat v. 8.3.

These tests must be run with the REST client (gpunit.client=REST) because 'reads.pair.1' and 'reads.pair.2' 
values are passed as an array. Array inputs are not supported by the SOAP client.

To get more accurate performance metrics, these test cases do not validate the job results.
This eliminates the need to download the result files from the GenePattern Server.


For best results set the following in your 'gpunit.default.properties' file:
    gpunit.client=REST
    gpunit.delete.jobs=false
    gpunit.save.downloads=false
    # the max amount of time in seconds to wait for each test to complete (4 hours)
    gpunit.jobCompletionTimeout=14400
    # the max number of GpUnit tests to run in parallel
    gpunit.numthreads=8
    
The tests in the 'short' folder are designed to complete quickly, so that you can verify 
the installation of the module and the configuration of your server and GpUnit client.

The test in the 'long' folder are designed to establish baseline performance metrics for
the TopHat module on the compute cluster.

    
