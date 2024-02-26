# Name Server
This module contains a name server that enables clients to find servers in the TupleSpace network.

## Installation
With a package installation we can also run the unittests. Simply go into the `src/` directory and run.
```
pip install .
```
Then, make sure to install the `Contract` library using Maven and with the virtual environment activated.

## Running
1 - Conclude the **Installation** proccess.

2 - Make sure the Python virtual environment is activated.

3 - Inside the `src/name_server/` directory run `python server.py`.

## Usage
```
usage: python server.py [-h] [--port PORT] [--debug]

Name server for TupleSpace distributed network

options:
  -h, --help   show this help message and exit
  --port PORT  port number where name server will be runnning (default: 5001)
  --debug      run in debug mode
```

## Testing
Simply run the following command in the `NameServer` directory with the virtual environment activated:
```
python -m unittest tests.test_name_server_state
```
