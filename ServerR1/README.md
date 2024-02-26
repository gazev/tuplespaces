This module contains the Server for the TupleSpaces network.
## Installation

Follow the installation process found in the root directory of the repository.
## Usage
```
Usage: mvn exec:java -Dexec.args="<port> <qualifier> [-h] [-d]"

Server for TuplesSpace distributed network

Positional arguments:
    port        Port where server will listen (default: 2001)
    qualifier   Server instance qualifier (default: A)
Options:
    -h, --help  Show this message and exit
    -d, --debug Run in debug mode
```