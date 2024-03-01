This module contains the Server for the TupleSpaces network.

## Installation

Follow the installation process found in the root directory of the repository.
## Usage
```
Usage: mvn exec:java -Dexec.args="<port> <qualifier> [host] [ns_host] [ns_port] [-h] [-d]"

Server for TuplesSpace distributed network

Positional arguments:
    port      Server port
    qualifier Server instance qualifier
Optional positional arguments:
    host      Server host IP address      (default: localhost)
    ns_host   Name server host IP address (default: localhost)
    ns_port   Name server port            (default: 5001)
Options:
    -h, --help  Show this message and exit
    -d, --debug Run in debug mode
```

## Notes

The "TupleSpaces" service name is a static variable that is shipped with the program.
The default values for optional arguments are defined at the start of the main function.