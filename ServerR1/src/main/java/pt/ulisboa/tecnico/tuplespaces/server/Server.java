package pt.ulisboa.tecnico.tuplespaces.server;

import static pt.ulisboa.tecnico.tuplespaces.server.ServerMain.debug;

import io.grpc.*;
import java.io.IOException;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import pt.ulisboa.tecnico.tuplespaces.server.grpc.NameServerService;
import pt.ulisboa.tecnico.tuplespaces.server.grpc.exceptions.NameServerRPCFailureException;

/**
 * Class encapsulating a TupleSpaces server.
 *
 * <p>The Class keeps an internal Tuple Space state, initially empty, and serves clients over gRPC.
 * See the full specification of the TupleSpaces server at
 * "https://github.com/tecnico-distsys/TupleSpaces/blob/master/tuplespaces.md"
 */
public class Server {
  private final String serviceName; // service name (e.g "TupleSpaces")
  private final String address; // server address
  private final String qualifier; // server qualifier (e.g, "A")

  private final NameServerService
      nameServerService; // class responsible for communication with the name server service
  private final ServerState state = new ServerState(); // server state
  private io.grpc.Server serverRef = null; // reference kept to perform shutdown logic on SIGINT

  public Server(
      String serviceName,
      String serverAddr,
      String qualifier,
      NameServerService nameServerService) {
    this.serviceName = serviceName;
    this.address = serverAddr;
    this.qualifier = qualifier;
    this.nameServerService = nameServerService;
  }

  /** Perform shutdown logic of the server. */
  public void shutdown() {
    debug("Call Server.shutdown(): No arguments");
    try {
      this.nameServerService.delete(
          this.serviceName, this.address); // unregister this server instance on the name server
    } catch (NameServerRPCFailureException e) {
      System.err.println("[WARN] Unable to unregister in name server on server shutdown");
      System.err.println(
          "[WARN] This may lead to unexpected side effects, such as invalid entries on the name server");
      System.err.printf("[ERROR] %s\n", e.getMessage());
    }

    this.nameServerService.shutdown();

    // shutdown gRPC server
    if (this.serverRef != null) this.serverRef.shutdown();
  }

  /**
   * Start running the server instance, registers in name server and block waiting for the gRPC
   * server termination or SIGINT.
   */
  public void run() {
    debug("Call Server.run(): No arguments");
    final BindableService impl = new TuplesSpaceServiceImpl(this.state);
    // NOTE we don't check for parseInt exceptions or Runtime exceptions because everything was
    // previously sanitized
    io.grpc.Server grpcServer =
        ServerBuilder.forPort(Integer.parseInt(this.address.split(":")[1]))
            .addService(impl)
            .build();
    serverRef = grpcServer; // save reference for shutdown logic

    // launch gRPC server
    try {
      grpcServer.start();
    } catch (IOException e) {
      System.err.println("[ERROR] Unable to launch gRPC server, unable to continue");
      System.err.printf("[ERROR] Error: %s\n", e.getMessage());
      grpcServer.shutdown();
      nameServerService.shutdown();
      System.exit(1);
      return;
    }

    // register server in name server
    try {
      nameServerService.register(this.serviceName, this.qualifier, this.address);
    } catch (NameServerRPCFailureException e) {
      System.err.println("[ERROR] Unable to register in name server, unable to continue");
      System.err.printf("[ERROR] %s\n", e.getMessage());
      grpcServer.shutdown();
      nameServerService.shutdown();
      System.exit(1);
      return;
    }

    System.out.printf(
        "[INFO] Running %s %s server on %s\n", this.serviceName, this.qualifier, this.address);

    // add hook to catch SIGINT and perform shutdown logic
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    // block awaiting gRPC server termination
    try {
      grpcServer.awaitTermination();
    } catch (InterruptedException e) {
      System.err.println("[ERROR] Server execution interrupted");
      System.err.printf("Error: %s\n", e.getMessage());
    }
  }
}
