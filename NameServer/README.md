# Name Server
This module contains a name server that enables clients to find servers in the TupleSpace network.

## Installation

### Manual
If you only want to get this working without running unit tests just do the following.
```
python -m virtualenv venv
source venv/bin/activate
pip install -r requirements.txt
```
Then, make sure to install the `Contract` library and it's all done.

### Package
A easier way is to install it as a local package, which will enable running the unittests too.
```
python -m virtualenv venv
source venv/bin/activate
pip install .
```
Then, make sure to install the `Contract` library and it's all done.

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
If the package is installed locally, running the unit tests is fairly simple, 
simply run the following command in the `NameServer` directory:
```
python -m unittest tests.test_name_server
```
