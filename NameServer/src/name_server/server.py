import sys
import argparse
from concurrent import futures
import traceback

sys.path += ['../../../Contract/target/generated-sources/protobuf/python']

import grpc
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc

from name_server.server_state import NameServerState

from name_server.server_exceptions import (
    InvalidDeleteRequest,
    InvalidRegisterRequest,
    InvalidServiceEntry
)


REGISTER_ERROR_MESSAGE = 'Not possible to register server'
DELETE_ERROR_MESSAGE   = 'Not possible to remove the server'


class NameServer(pb2_grpc.NameServerServicer):
    """ 
    gRPC NameServer subtype class.

    This class is a subtype of the gRPC NameServerServicer and provides
    implementation for handling specific gRPC service methods. It serves
    as the server-side implementation for the NameServer service.

    See pt.ulisboa.tecnico.tuplespaces.namesever.
    """
    def __init__(self):
        self.state = NameServerState() # internal state
    
    def Lookup(self, request, ctx):
        """ Lookup RPC, see pt.ulisboa.tecnico.tuplespaces.nameserver """
        debug("Running Lookup procedure. " + \
            f"Arguments: ServiceName={request.ServiceName}, Qualifier={request.Qualifier}")

        # build list of ServiceEntries response (list of tuples with a qualifier and address)
        entries = self.state.lookup(request.ServiceName, request.Qualifier)

        entries_response = [
            pb2.LookupResponse.ServiceEntry(Qualifier=entry.qual, ServiceAddress=entry.addr)
            for entry in entries 
        ]

        return pb2.LookupResponse(ServiceEntries=entries_response)
    
    def Register(self, request, ctx):
        """ Register RPC, see pt.ulisboa.tecnico.tuplespaces.nameserver """
        debug("Running Register procedure. " + \
            f"Arguments: ServiceName={request.ServiceName}, Qualifier={request.Qualifier}, address={request.Address}")
        try:
            self.state.register(request.ServiceName, request.Qualifier, request.Address)
        except InvalidServiceEntry as e: # invalid params
            debug(f"InvalidServiceEntry {str(e)}")
            return ctx.abort(grpc.StatusCode.INVALID_ARGUMENT, REGISTER_ERROR_MESSAGE)
        except InvalidRegisterRequest as e: # service entry already exists
            debug(f"InvalidRegisterRequest {str(e)}")
            return ctx.abort(grpc.StatusCode.ALREADY_EXISTS, REGISTER_ERROR_MESSAGE)
        
        debug(f"Registered new service: ServiceName={request.ServiceName}, Qualifier={request.Qualifier}, Address={request.Address}")
        return pb2.RegisterResponse()

    def Delete(self, request, ctx):
        """ Delete RPC, see pt.ulisboa.tecnico.tuplespaces.nameserver """
        debug("Running Delete procedure. " + \
            f"Arguments: ServiceName={request.ServiceName}, Address={request.Address}")
        try:
            self.state.delete(request.ServiceName, request.Address)
        except InvalidServiceEntry as e: # invalid params
            debug(f"InvalidServiceEntry {str(e)}")
            return ctx.abort(grpc.StatusCode.INVALID_ARGUMENT, DELETE_ERROR_MESSAGE)
        except InvalidDeleteRequest as e: # service entry doesn't exist
            debug(f"InvalidDeleteRequest {str(e)}")
            return ctx.abort(grpc.StatusCode.NOT_FOUND, DELETE_ERROR_MESSAGE)
        
        debug(f"Deleted service: ServiceName={request.ServiceName}, Address={request.Address}")
        return pb2.DeleteResponse()
    
    def run(self, port: int):
        """ Program's entry point, runs gRPC NameServer service on port ``port`` """
        server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
        pb2_grpc.add_NameServerServicer_to_server(self, server)
        server.add_insecure_port(f'localhost:{port}')

        server.start()
        print(f'Running server on port {port}')
        try:
            server.wait_for_termination()
        except KeyboardInterrupt:
            print("\nSIGINT received, terminating...")


DEBUG_MODE = False
def debug(s: str):
    global DEBUG_MODE
    if (DEBUG_MODE):
        print(f"[DEBUG] {s}", file=sys.stderr)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Name server for TupleSpace distributed network')
    parser.add_argument(
        '--port', 
        help='port number where name server will be runnning (default: %(default)s)',
        type=int, 
        default=5001
    )
    # add a --debug argument to enable debugging
    parser.add_argument(
        '--debug', 
        help='run in debug mode',
        action='store_true', 
    )

    args = parser.parse_args()

    if not (1024 < int(args.port) < 2**16):
        parser.error(f'argument --port: invalid port value: {args.port}')
        exit(1)

    DEBUG_MODE = args.debug
    if DEBUG_MODE:
        print("Debug mode activated")
    
    server = NameServer().run(args.port)

