package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.BindableService;
import io.grpc.ServerBuilder;

import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

import static pt.ulisboa.tecnico.tuplespaces.server.ServerMain.debug;

/**
 * Class encapsulating a TupleSpaces server.
 *
 * <p>The server keeps an internal Tuple Space state, initially empty, and serves clients over gRPC.
 * See the full specification of the server @ <a
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
  private String host;
  private int port;
  private String service;
  private String qual;

  private ServerState state = new ServerState();

  /**
   * @param host Server host address
   * @param port Server port
   * @param qual Server qualifier
   * @param service Server service name
   */
  public Server(String host, int port, String service, String qual) {
    this.host = host;
    this.port = port;
    this.service = service;
    this.qual = qual;
  }

  /**
   * Start running the server instance and blocks waiting for gRPC server termination or until
   * SIGINT is received.
   */
  public void run() {
    debug(
        String.format(
            "Running server instance. Info: Service: %s, ID: %s on %s:%d",
            this.service, this.qual, this.host, this.port));
    final BindableService impl = new TuplesSpaceServiceImpl(state);

    io.grpc.Server server = ServerBuilder.forPort(port).addService(impl).build();

    // add hook to catch SIGINT and perform shutdown logic
    Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

    System.out.printf("Running TupleSpaces server on %s:%d\n", host, port);
    try {
      server.awaitTermination();
    } catch (InterruptedException e) {
      System.err.println(e.getMessage());
    }
  }

  /**
   * Registers the Server in the Name Server
   *
   * @param nameServerAddr string in the format host:port identifying the Name Server
   * @throws
   */
  public void registerInNameServer(String nameServerAddr) {
    debug("REGISTER");
  }

  /**
   * Unregisters the Server in the Name Server
   *
   * @param nameServerAddr string in the format host:port identifying the Name Server
   * @throws
   */
  public void unregisterInNameServer(String nameServerAddr) {
    debug("UNREGISTER");
  }
}
