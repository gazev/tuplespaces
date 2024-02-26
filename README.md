# TupleSpaces

Distributed Systems Project 2024


<!-- *(fill the line below with the group identifier, for example A25 or T25, and then delete this line)*   -->
**Group GA34**

<!-- *(choose one of the following levels and erase the other one)*   -->
**Difficulty level: I am Death incarnate!**


<!-- ### Code Identification

In all source files (namely in the *groupId*s of the POMs), replace __GXX__ with your group identifier. The group
identifier consists of either A or T followed by the group number - always two digits. This change is important for 
code dependency management, to ensure your code runs using the correct components and not someone else's.
 -->
### Team Members

<!-- *(fill the table below with the team members, and then delete this line)* -->

| Number | Name              | User                             | Email                               |
|--------|-------------------|----------------------------------|-------------------------------------|
| 93075  | Gonçalo Azevedo  | <https://github.com/gazev>   | <mailto:goncalo.r.azevedo@tecnico.ulisboa.pt>   |
| 99986  | João Tiago       | <https://github.com/jmlt2002>     | <mailto:joao.leal.tintas@tecnico.ulisboa.pt>     |
| 103156  | Francisco Nael Salgado     | <https://github.com/fnael> | <mailto:francisco.nael.salgado@tecnico.ulisboa.pt> |

## Getting Started

The overall system is made up of several modules. The different types of servers are located in _ServerX_ (where X denotes stage 1, 2 or 3). 
The clients is in _Client_.
The definition of messages and services is in _Contract_. The future naming server
is in _NamingServer_.

See the [Project Statement](https://github.com/tecnico-distsys/TupleSpaces) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```sh
javac -version
mvn -version
```


The Project was also developed using Python 3.11.7, pip 24.0 and a Python virtual environment module. Higher versions of Python are expected to work, if not, you can use `pyenv`. 

To confirm that you have them installed and which version they are, run in the terminal:
```sh
python --version
python -m pip --version
python -m virtualenv --version
```

### Installation

First and foremost, the protocol buffer and gRPC classes must be generated. Because we have a Python module using gRPC, we will need the `protobuf` and `grpcio` Python dependencies installed.

First create a Python virtual environment and activate it running the following commands:
```sh
python -m virtualenv venv
source venv/bin/activate
```
Keep the virtual environment activated for the entire installation process.

Now, navigate to the `NameServer/src` directory and install the `name_server` package (this will also install the protobuf and gRPC dependencies) running in the terminal:
```
pip install .
```

Once the Python dependencies are installed, install the `Contract` module. Go into the `Contract/` directory and run the following command:
```sh
mvn install exec:exec
```

---

Now the libraries are ready and each module can be compiled and ran individually.

#### Client
Inside the `Client/` directory run:
```sh
mvn install 
```

#### Server
Inside the `ServerR1/` directory run:
```sh
mvn install 
```

#### NameServer
The name server was already installed with the previous `pip install .` command. To run it, simply keep the virtual environment activated and run the following command inside the `NameServer/src/name_server/` directory:
```s
python server.py
```

## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.
* [pip](https://pypi.org/project/pip/) - Python package manager

