# Name Server
This module contains a name server that enables clients to find servers in the TupleSpace network.

## Installation

### Manual
Create a virtual environment and install the requirements specified in `requirements.txt` in the root directory of the TupleSpace project as follows:
```
python -m virtualenv venv
source venv/bin/activate
pip install -r requirements.txt
```
Then, install the `Contract` library using Maven and with the virtual environment activated.

### Package
With a package installation you can also run the unittests. Simply go into the `src/name_server` directory and run.
```
python -m virtualenv venv
source venv/bin/activate
pip install .
```
Then, make sure to install the `Contract` library using Maven and with the virtual environment actiavted.

## Running
1 - Conclude the **Installation** proccess.

2 - Make sure the Python virtual environment is activated.

3 - Inside the `src/name_server` directory run `python server.py`.

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
If you installed using the package, running the unit tests is fairly simple.
Simply run the following command in the `NameServer` directory:
```
python -m unittest tests.test_name_server_state
```
