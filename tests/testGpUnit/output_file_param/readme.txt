This test is for a bug in GpUnit which can happen under the following conditions:

1. the test.yaml file is in a sub directory
2. the test.yaml file has a text input parameter which is set to 
   the name of the sub directory

You must run the test from this parent directory. For example,
    gpunit ...  -Dgpunit.includes=output_file.txt/output_file_param.yml gpunit
