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
    public final String qualifier; // server qualifier
    public final String address;   // server address
    public ManagedChannel channel;
    public TupleSpacesGrpc.TupleSpacesBlockingStub stub;

    public ServerEntry(String address, String qualifier) {
      this.address = address;
      this.qualifier = qualifier;

      setup();
    }

    /** Create channel and stub for given server */
    private void setup() {
      debug("Call ServerService::setup");
      this.channel = ManagedChannelBuilder.forTarget(this.address).usePlaintext().build();
      this.stub = TupleSpacesGrpc.newBlockingStub(this.channel);
    }

    public String getAddress() {
      return this.address;
    }

    public String getQualifier() {
      return this.qualifier;
    }

    /** Perform server shutdown logic */
    public void shutdown() {
      debug("Call ServerService::shutdown");
      this.channel.shutdown();
    }

    @Override
    public String toString() {
      return String.format("{address=%s, qualifier=%s}", this.address, this.qualifier);
    }
  }

  private List<ServerEntry> serverEntries;

  /**
   * Constructor when no services are found
   */
  public TuplesSpacesService() {}

  /**
   * Constructor when we already fetched servers from the name server
   *
   * @param serverEntries ServiceEntry list with all available servers
   */
  public TuplesSpacesService(List<NameServerService.ServiceEntry> serverEntries) {
    setServers(serverEntries);
  }

  /**
   * Add servers to the Server Entries list
   *
   * @param serverEntries List of server entries retrieved from name server lookup procedure
   */
  public void setServers(List<NameServerService.ServiceEntry> serverEntries) {
    debug("Call TupleSpacesService::setServers");
    this.serverEntries.clear();
    for (NameServerService.ServiceEntry server : serverEntries) {
      this.serverEntries.add(new ServerEntry(server.getAddress(), server.getQualifier()));
    }
  }

  /**
   * Add a single server to the Server Entries list
   *
   * @param server ServerEntry object
   */
  public void addServer(NameServerService.ServiceEntry server) {
    debug(String.format("Call TupleSpacesService::addServer: serverEntry=%s", server.toString()));
    this.serverEntries.add(new ServerEntry(server.getAddress(), server.getQualifier()));
  }

  /**
   * Get a server from the Server Entries list
   *
   * @param qualifier Server qualifier
   * @return ServerEntry object
   */
  public ServerEntry getServer(String qualifier) {
    debug(String.format("Call TupleSpacesService::getServer: qualifier=%s", qualifier));
    for (ServerEntry server : this.serverEntries) {
      if (server.getQualifier().equals(qualifier)) {
        return server;
      }
    }
    return null;
  }

  /**
   * Get all servers from the Server Entries list
   *
   * @return List of ServerEntry objects
   */
  public List<ServerEntry> getServers() {
    debug("Call TupleSpacesService::getServers");
    return this.serverEntries;
  }

  /**
   *  Returns true if there are servers currently available
   */
  public boolean hasServers() {
    debug("Call TupleSpacesService::hasServers");
    return (this.serverEntries.size() > 0);
  }

  /**
   * Removes one server from the Server Entries list
   * 
   * @param qualifier Server qualifier
   */
  public void removeSingleServer(String qualifier) {
    debug(String.format("Call TupleSpacesService::removeSingleServer: qualifier=%s", qualifier));
    ServerEntry server = getServer(qualifier);
    if (server != null) {
      server.shutdown();
      this.serverEntries.remove(server);
    }
  }

  /**
   * Perform shutdown logic
   */
  public void shutdown() {
    debug("Call TupleSpacesService::shutdown");
    for (ServerEntry server : this.serverEntries) {
      server.shutdown();
      this.serverEntries.remove(server);
    }
  }

  /**
   * TupleSpaces 'put' gRPC wrapper.
   *
   * @param tuple String of the tuple we wish to save to the server
   * @throws TupleSpacesServiceRPCFailureException on RPC failure or invalid request parameters
   */
  public void put(String tuple) throws TupleSpacesServiceRPCFailureException {
    debug(String.format("Call TuplesSpacesService::put: tuple=%s", tuple));
    for (ServerEntry server : this.serverEntries) {
      try {
        // we ignore the return value because it's an empty response
        server.stub.put(TupleSpacesCentralized.PutRequest.newBuilder().setNewTuple(tuple).build());
      } catch (StatusRuntimeException e) {
        debug(e.getMessage());
        throw new TupleSpacesServiceRPCFailureException("Put", e.getStatus().getDescription());
      }
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
    debug(String.format("Call TuplesSpacesService::read: searchPattern=%s", searchPattern));
    TupleSpacesCentralized.ReadResponse response = null;
    for (ServerEntry server : this.serverEntries) {
      try {
        response =
            server.stub.read(
                TupleSpacesCentralized.ReadRequest.newBuilder()
                    .setSearchPattern(searchPattern)
                    .build());
      } catch (StatusRuntimeException e) {
        debug(e.getMessage());
        throw new TupleSpacesServiceRPCFailureException("Read", e.getStatus().getDescription());
      }
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
  /*
  public String take(String searchPattern) throws TupleSpacesServiceRPCFailureException {
    debug("Call TuplesSpacesService::take: searchPattern=" + searchPattern);
    TupleSpacesCentralized.TakeResponse response = null;
    for (ServerEntry server : this.serverEntries) {
      try {
        response =
            server.stub.take(
                TupleSpacesCentralized.TakeRequest.newBuilder()
                    .setSearchPattern(searchPattern)
                    .build());
      } catch (StatusRuntimeException e) {
        debug(e.getMessage());
        throw new TupleSpacesServiceRPCFailureException("Take", e.getStatus().getDescription());
      }
    }
    // return first result
    return response.getResult();
  }
  */

  /**
   * TupleSpaces 'getTupleSpacesState' gRPC wrapper.
   *
   * @return String representation of the list with all tuples in the server
   * @throws TupleSpacesServiceRPCFailureException on RPC failure
   */
  public String getTupleSpacesState(String qualifier) throws TupleSpacesServiceRPCFailureException {
    debug("Call TuplesSpacesService::getTupleSpacesState: qualifier=" + qualifier);
    TupleSpacesCentralized.getTupleSpacesStateResponse response = null;
    for (ServerEntry server : this.serverEntries) {
      if (server.getQualifier().equals(qualifier)) {
        try {
          response =
              server.stub.getTupleSpacesState(
                  TupleSpacesCentralized.getTupleSpacesStateRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
          debug(e.getMessage());
          throw new TupleSpacesServiceRPCFailureException(
              "GetTupleSpacesState", e.getStatus().getDescription());
        }
      }
    }
    return response.getTupleList().toString();
  }
}
