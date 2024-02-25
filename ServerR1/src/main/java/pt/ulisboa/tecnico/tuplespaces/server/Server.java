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
 * <p>The Class keeps an internal Tuple Space state, initially empty, and serves clients over gRPC.
 * See the full specification of the TupleSpaces server <a
 * href="https://github.com/tecnico-distsys/TupleSpaces/blob/master/tuplespaces.md">here</a>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Server tupleServer = new Server("localhost", "2001", "A", "TupleSpaces");
 * tupleServer.registerInNameServer();
 * tupleServer.run();
 * tupleServer.unregisterInNameServer();
 * }</pre>
 */
public class Server {
  private final String host;    // server host      (e.g, "localhost")
  private final int port;       // server port      (e.g, 2001)
  private final String service; // service name     (e.g, "TupleSpace")
  private final String qual;    // server qualifier (e.g, "A")

  private NameServerService nameServerService = null;  // composition, initialized w dependency injection
  private final ServerState state = new ServerState(); // server state
  private io.grpc.Server serverRef = null; // reference kept to perform shutdown logic on SIGINT

  public Server(
      String host, int port, String qual, String service, NameServerService nameServerService) {
    this.host = host;
    this.port = port;
    this.qual = qual;
    this.service = service;
    this.nameServerService = nameServerService;
  }

  /**
   * Perform server initialization logic.
   */
  public void setup() {
    debug("Call Server.connect: No arguments");
    this.nameServerService.connect();
  }

  /**
   * Perform shutdown logic of the server.
   */
  public void shutdown() {
    debug("Call Server.shutdown: No arguments");
    try {
      unregister(); // unregister our server instance on the name server
    } catch (ServerUnregisterException e) {
      debug(e.getMessage());
      System.err.println("[WARN] Failed unregistering server.");
      System.err.println("[WARN] This will result in an invalid entry on the name server.");
    }

    nameServerService.shutdown();
    // shutdown gRPC server
    if (serverRef != null) serverRef.shutdown();
  }

  /**
   * Register server instance in the name server.
   *
   * @throws ServerRegisterException if registration cannot be completed.
   */
  public void register() throws ServerRegisterException {
    debug("Call Server.register: No arguments");
    try {
      this.nameServerService.register(this.service, this.qual, this.host + ":" + this.port);
    } catch (NameServerRPCFailureException e) {
      debug(e.getMessage());
      throw new ServerRegisterException(this.service, this.qual, this.host + ":" + this.port);
    }
  }

  /**
   * Unregister server instance in the name server.
   *
   * @throws ServerUnregisterException if the unregistration cannot be completed.
   */
  public void unregister() throws ServerUnregisterException {
    debug("Call Server.delete: No arguments");
    try {
      this.nameServerService.delete(this.service, host + ":" + this.port);
    } catch (NameServerRPCFailureException e) {
      debug(e.getMessage());
      throw new ServerUnregisterException(this.service, this.host + ":" + this.port);
    }
  }

  /**
   * Start running the server instance and blocks waiting for the gRPC server termination or SIGINT.
   */
  public void run() {
    debug("Call Server.run: No arguments");

    final BindableService impl = new TuplesSpaceServiceImpl(state);
    io.grpc.Server grpcServer = ServerBuilder.forPort(port).addService(impl).build();

    serverRef = grpcServer; // save reference for shutdown logic
    // add hook to catch SIGINT and perform shutdown logic
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

    try {
      grpcServer.start();
    } catch (IOException e) {
      debug(e.getMessage());
      System.err.println("[ERROR] Failed launching server.");
      return;
    }

    System.out.printf("[INFO] Running %s %s server on %s:%s.\n", service, qual, host, port);
    try {
      grpcServer.awaitTermination(); // blocks here
    } catch (InterruptedException e) {
      debug(e.getMessage());
      System.err.println("[ERROR] Server execution interrupted.");
    }
  }
}
