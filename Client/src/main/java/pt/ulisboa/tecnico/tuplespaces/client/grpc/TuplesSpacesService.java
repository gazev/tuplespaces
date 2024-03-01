package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.TupleSpacesServiceRPCFailureException;

/** TuplesSpacesService class encapsulates the gRPC interface of the TupleSpaces service client. */
public class TuplesSpacesService {

  /** ServerEntry class represents a gRPC server of the TupleSpaces network */
  public static class ServerEntry {
    public final String address;   // server address
    public final String qualifier; // server qualifier
    public ManagedChannel channel;
    public TupleSpacesGrpc.TupleSpacesBlockingStub stub;

    public ServerEntry(String address, String qualifier) {
      this.address = address;
      this.qualifier = qualifier;

      setup();
    }

    /** Create channel and stub for given server */
    private void setup() {
      debug("Call ServerService.connect()");
      this.channel = ManagedChannelBuilder.forTarget(this.address).usePlaintext().build();
      this.stub = TupleSpacesGrpc.newBlockingStub(this.channel);
    }

    /** Server shutdown logic */
    public void shutdown() {
      debug("Call ServerService.shutdown()");
      this.channel.shutdown();
    }
  }

  private ServerEntry
      server; // server we are talking to, only one now, TODO change for second phase

  /**
   * Constructor when no services are found
   */
  public TuplesSpacesService() {
  }

  /**
   * Constructor when we already have servers
   *
   * @param serverEntries ServiceEntry list with all available servers fetched from name server
   */
  public TuplesSpacesService(List<NameServerService.ServiceEntry> serverEntries) {
    setServer(serverEntries);
  }

  /**
   * Set the Server the client is talking to
   * // TODO replace with addServer for second phase
   *
   * @param serverEntries List of server entries retrieved from name server lookup procedure
   */
  public void setServer(List<NameServerService.ServiceEntry> serverEntries) {
    this.server = new ServerEntry(serverEntries.get(0).getAddress(), serverEntries.get(0).getQualifier());
  }

  /** Returns true if there are servers currently available
   *  TODO in second phase check if list is empty
   */
  public boolean hasServers() {
    return this.server == null;
  }

  /** Remove current server in use
   * TODO in second phase remove by index or element
   * */
  public void removeCurrentServer() {
    debug("TupleSpacesService.removeCurrentServer()");
    if (hasServers()) {
      this.server.shutdown();
      this.server = null;
    }
  }

  /** Perform shutdown logic */
  public void shutdown() {
    debug("TupleSpacesService.shutdown()");
    removeCurrentServer();
  }

  /**
   * TupleSpaces 'put' gRPC wrapper.
   *
   * @param tuple String of the tuple we wish to save to the server
   * @throws TupleSpacesServiceRPCFailureException on RPC failure or invalid request parameters
   */
  public void put(String tuple) throws TupleSpacesServiceRPCFailureException {
    debug("Call TuplesSpacesService.put(): tuple=" + tuple);
    try {
      // we ignore the return value because it's an empty response
      this.server.stub.put(TupleSpacesCentralized.PutRequest.newBuilder().setNewTuple(tuple).build());
    } catch (StatusRuntimeException e) {
      debug(e.getMessage());
      throw new TupleSpacesServiceRPCFailureException("Put", e.getStatus().getDescription());
    }
  }

  /**
   * TupleSpaces 'read' gRPC wrapper.
   *
   * @param searchPattern A regex pattern (or simply a string) that matches the tuple we want to read from the server.
   * @return String representation of the tuple read from the server
   * @throws TupleSpacesServiceRPCFailureException on RPC failure or invalid request parameters
   */
  public String read(String searchPattern) throws TupleSpacesServiceRPCFailureException {
    debug("Call TuplesSpacesService.read(): searchPattern=" + searchPattern);
    TupleSpacesCentralized.ReadResponse response = null;
    try {
      response =
          this.server.stub.read(
              TupleSpacesCentralized.ReadRequest.newBuilder()
                  .setSearchPattern(searchPattern)
                  .build());
    } catch (StatusRuntimeException e) {
      debug(e.getMessage());
      throw new TupleSpacesServiceRPCFailureException("Read", e.getStatus().getDescription());
    }
    // return first result
    return response.getResult();
  }

  /**
   * TupleSpaces 'take' gRPC wrapper.
   *
   * @param searchPattern A regex pattern (or simply a string) that matches the tuple we want to read from the server.
   * @return String representation of the tuple read from the server
   * @return String representation of the tuple taken from the server
   * @throws TupleSpacesServiceRPCFailureException on RPC failure or invalid request parameters
   */
  public String take(String searchPattern) throws TupleSpacesServiceRPCFailureException {
    debug("Call TuplesSpacesService.take(): searchPattern=" + searchPattern);
    TupleSpacesCentralized.TakeResponse response = null;
    try {
      response =
          this.server.stub.take(
              TupleSpacesCentralized.TakeRequest.newBuilder()
                  .setSearchPattern(searchPattern)
                  .build());
    } catch (StatusRuntimeException e) {
      debug(e.getMessage());
      throw new TupleSpacesServiceRPCFailureException("Take", e.getStatus().getDescription());
    }
    // return first result
    return response.getResult();
  }

  /**
   * TupleSpaces 'getTupleSpacesState' gRPC wrapper.
   *
   * @return String representation of the list with all tuples in the server
   * @throws TupleSpacesServiceRPCFailureException on RPC failure
   */
  public String getTupleSpacesState() throws TupleSpacesServiceRPCFailureException {
    debug("Call TuplesSpacesService.getTupleSpacesState()");
    TupleSpacesCentralized.getTupleSpacesStateResponse response = null;
    try {
      response =
          this.server.stub.getTupleSpacesState(
              TupleSpacesCentralized.getTupleSpacesStateRequest.getDefaultInstance());
    } catch (StatusRuntimeException e) {
      debug(e.getMessage());
      throw new TupleSpacesServiceRPCFailureException(
          "GetTupleSpacesState", e.getStatus().getDescription());
    }

    return response.getTupleList().toString();
  }
}
