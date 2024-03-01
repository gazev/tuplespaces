package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.*;

import static pt.ulisboa.tecnico.tuplespaces.server.ServerMain.debug;

import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import pt.ulisboa.tecnico.tuplespaces.server.exceptions.ServerRegisterException;
import pt.ulisboa.tecnico.tuplespaces.server.exceptions.ServerUnregisterException;
import pt.ulisboa.tecnico.tuplespaces.server.grpc.NameServerService;
import pt.ulisboa.tecnico.tuplespaces.server.grpc.exceptions.NameServerRPCFailureException;

import java.io.IOException;

/**
 * Class encapsulating a TupleSpaces server.
 *
 * The Class keeps an internal Tuple Space state, initially empty, and serves clients over gRPC.
 * See the full specification of the TupleSpaces server at
 * "https://github.com/tecnico-distsys/TupleSpaces/blob/master/tuplespaces.md"
 */
public class Server {
  private final String serviceName; // service name (e.g "TupleSpaces")
  private final String address;     // server address
  private final String qualifier;   // server qualifier (e.g, "A")

  private final NameServerService nameServerService;   // class responsible for communication with the name server service
  private final ServerState state = new ServerState(); // server state
  private io.grpc.Server serverRef = null;             // reference kept to perform shutdown logic on SIGINT

  public Server(String serviceName,
      String serverAddr, String qualifier, NameServerService nameServerService) {
    this.serviceName = serviceName;
    this.address = serverAddr;
    this.qualifier = qualifier;
    this.nameServerService = nameServerService;
  }

  /** Perform shutdown logic of the server. */
  public void shutdown() {
    debug("Call Server.shutdown(): No arguments");
    try {
      unregister(); // unregister our server instance on the name server
    } catch (ServerUnregisterException e) {
      debug(e.getMessage());
      System.err.println("[WARN] Failed unregistering server");
      System.err.println("[WARN] This may lead to unexpected side effects, such as invalid entries on the name server");
    }

    this.nameServerService.shutdown();
    // shutdown gRPC server
    if (this.serverRef != null) this.serverRef.shutdown();
  }

  /**
   * Register server instance in the name server.
   *
   * @throws ServerRegisterException if registration cannot be completed
   */
  public void register() throws ServerRegisterException {
    debug("Call Server.register(): No arguments");
    try {
      this.nameServerService.register(this.serviceName, this.qualifier, this.address);
    } catch (NameServerRPCFailureException e) {
      throw new ServerRegisterException(this.serviceName, this.qualifier, this.address, e.getMessage());
    }
  }

  /**
   * Unregister server instance in the name server.
   *
   * @throws ServerUnregisterException if the deletion cannot be completed
   */
  public void unregister() throws ServerUnregisterException {
    debug("Call Server.delete(): No arguments");
    try {
      this.nameServerService.delete(this.serviceName, this.address);
    } catch (NameServerRPCFailureException e) {
      throw new ServerUnregisterException(this.serviceName, this.address);
    }
  }

  /**
   * Start running the server instance and block waiting for the gRPC server termination or SIGINT.
   */
  public void run() {
    debug("Call Server.run(): No arguments");
    final BindableService impl = new TuplesSpaceServiceImpl(this.state);
    // NOTE we don't check for parseInt exceptions or Runtime exceptions on [1] because everything was previously sanitized
    io.grpc.Server grpcServer = ServerBuilder.forPort(Integer.parseInt(this.address.split(":")[1])).addService(impl).build();
    serverRef = grpcServer; // save reference for shutdown logic

    // add hook to catch SIGINT and perform shutdown logic
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    // launch gRPC server
    try {
      grpcServer.start();
    } catch (IOException e) {
      System.err.println("[ERROR] Failed launching server. Error: " + e.getMessage());
      return;
    }

    System.out.printf("[INFO] Running %s %s server on %s\n", this.serviceName, this.qualifier, this.address);
    // block awaiting gRPC server termination
    try {
      grpcServer.awaitTermination();
    } catch (InterruptedException e) {
      System.err.println("[ERROR] Server execution interrupted. Error: " + e.getMessage());
    }
  }
}
