# Gzoltar Gradle Plugin
# Tasks
The plugin contains the following tasks:
  * gzoltarGenTestList : Generates a list of test cases for gzoltar to run
  * gzoltarDownloads : Downloads the jar files required to run gzoltar
  * gzoltarRunTests : Runs the testsuite and collects the coverage data
  * gzoltarReport : Generates a gzoltar SBFL report
  * flitsr : Generates a FLITSR ranking report
# Extension
The plugin has the following options provided in the plugins extension:
  * testMethods : The name of the file to store the list of tests to run
  * serFile : The name of the file to store the serialized coverage data
  * granularity : The granularity level of the coverage generated
  * outFile : The output file for the FLITSR results
  * includes : The tests to include in the testsuite running
