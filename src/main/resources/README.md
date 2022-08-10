# FLITSR: Fault Localization with Iterative Test Suite Reduction
FLITSR is an extension to Spectrum Based fault Localization that allows the
simultaneous debugging of multiple faults.

FLITSR consists of an iterative technique that considers reduced forms of the
original test suite that more accurately identify the individual faults in the
system. FLITSR requires only the spectral counts given to the SBFL technique,
and thus works seemlessly on top of original SBFL tools.

# Technologies
This implementation of FLITSR has been developed as a python application that
runs on top of any coverage collection tool.
# Usage
To use this tool, simply collect the coverage spectra using any prefered
coverage tool, and run this application on the results.

NOTE: You may need to create a new input method if the coverage tool you are
using is not supported.
# Running
To run this tool, use the command:
```
./flitsr <input file> [<options>]
```
where a full list of available options is given by running the command:
```
./flitsr
```
For convenience, the script can be added to the users PATH so that it can be run
from anywhere. To do so, simply add the following line to your `.bashrc`:
```
export PATH="$HOME/path/to/flitsr/directory:$PATH"
```
where `path/to/flitsr/directory` is the system path to this directory.
