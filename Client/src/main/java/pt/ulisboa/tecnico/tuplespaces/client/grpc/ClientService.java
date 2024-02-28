package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.ClientServiceRPCFailureException;

/** ClientService class encapsulates the gRPC interface of the TupleSpaces service client. */
public class ClientService {

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
      debug("Call ServerService.connect");
      this.channel = ManagedChannelBuilder.forTarget(this.address).usePlaintext().build();
      this.stub = TupleSpacesGrpc.newBlockingStub(this.channel);
    }

    /** Server shutdown logic */
    public void shutdown() {
      debug("Call ServerService.shutdown");
      this.channel.shutdown();
    }
  }

  private ServerEntry
      server; // server we are talking to, only one now, TODO change for second phase

  /**
   * @param serverEntries ServiceEntry list with all available servers fetched from name server
   */
  public ClientService(List<NameServerService.ServiceEntry> serverEntries) {
    setServer(serverEntries.get(0)); // TODO change for second phase where we call addServer for all fetched servers
  }

  /**
   * Set the Server the client is talking to
   * // TODO replace with addServer for second phase
   *
   * @param serverEntry NameServerService.ServiceEntry which represents a server fetched from the name server
   */
  public void setServer(NameServerService.ServiceEntry serverEntry) {
    this.server = new ServerEntry(serverEntry.getAddress(), serverEntry.getQualifier());
  }

  /** Perform shutdown logic */
  public void shutdown() {
    debug("Call ClientService.shutdown");
    if (this.server != null) server.shutdown();
  }

  /**
   * TupleSpaces 'put' gRPC wrapper.
   *
   * @param tuple String of the tuple we wish to save to the server
   * @throws ClientServiceRPCFailureException on RPC failure or invalid request parameters
   */
  public void put(String tuple) throws ClientServiceRPCFailureException {
    debug("Call ClientService.put: tuple=" + tuple);
    try {
      // we ignore the return value because it's an empty response
      this.server.stub.put(TupleSpacesCentralized.PutRequest.newBuilder().setNewTuple(tuple).build());
    } catch (StatusRuntimeException e) {
      debug(e.getMessage());
      throw new ClientServiceRPCFailureException("Put", e.getStatus().getDescription());
    }
  }

  /**
   * TupleSpaces 'read' gRPC wrapper.
   *
   * @param searchPattern A regex pattern (or simply a string) that matches the tuple we want to read from the server.
   * @return String representation of the tuple read from the server
   * @throws ClientServiceRPCFailureException on RPC failure or invalid request parameters
   */
  public String read(String searchPattern) throws ClientServiceRPCFailureException {
    debug("Call ClientService.read: searchPattern=" + searchPattern);
    TupleSpacesCentralized.ReadResponse response = null;
    try {
      response =
          this.server.stub.read(
              TupleSpacesCentralized.ReadRequest.newBuilder()
                  .setSearchPattern(searchPattern)
                  .build());
    } catch (StatusRuntimeException e) {
      debug(e.getMessage());
      throw new ClientServiceRPCFailureException("Read", e.getStatus().getDescription());
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
   * @throws ClientServiceRPCFailureException on RPC failure or invalid request parameters
   */
  public String take(String searchPattern) throws ClientServiceRPCFailureException {
    debug("Call ClientService.take: searchPattern=" + searchPattern);
    TupleSpacesCentralized.TakeResponse response = null;
    try {
      response =
          this.server.stub.take(
              TupleSpacesCentralized.TakeRequest.newBuilder()
                  .setSearchPattern(searchPattern)
                  .build());
    } catch (StatusRuntimeException e) {
      debug(e.getMessage());
      throw new ClientServiceRPCFailureException("Take", e.getStatus().getDescription());
    }
    // return first result
    return response.getResult();
  }

  /**
   * TupleSpaces 'getTupleSpacesState' gRPC wrapper.
   *
   * @return String representation of the list with all tuples in the server
   * @throws ClientServiceRPCFailureException on RPC failure
   */
  public String getTupleSpacesState() throws ClientServiceRPCFailureException {
    debug("Call ClientService.getTupleSpacesState");
    TupleSpacesCentralized.getTupleSpacesStateResponse response = null;
    try {
      response =
          this.server.stub.getTupleSpacesState(
              TupleSpacesCentralized.getTupleSpacesStateRequest.getDefaultInstance());
    } catch (StatusRuntimeException e) {
      debug(e.getStatus().getDescription());
      throw new ClientServiceRPCFailureException(
          "GetTupleSpacesState", e.getStatus().getDescription());
    }

    return response.getTupleList().toString();
  }
}
