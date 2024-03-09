package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;

/** TuplesSpacesService class encapsulates the gRPC interface of the TupleSpaces service client. */
public class TuplesSpacesService {

  /** ServerEntry class represents a gRPC server of the TupleSpaces network */
  public static class ServerEntry {
    public final String qualifier; // server qualifier
    public final String address; // server address
    public ManagedChannel channel;
    public TupleSpacesGrpc.TupleSpacesStub stub;

    public ServerEntry(String address, String qualifier) {
      this.address = address;
      this.qualifier = qualifier;

      setup();
    }

    /** Create channel and stub for given server */
    private void setup() {
      debug(String.format("Call ServerService::setup: serverEntry=%s", this.toString()));
      this.channel = ManagedChannelBuilder.forTarget(this.address).usePlaintext().build();
      this.stub = TupleSpacesGrpc.newStub(this.channel);
    }

    public String getAddress() {
      return this.address;
    }

    public String getQualifier() {
      return this.qualifier;
    }

    /** Perform server shutdown logic */
    public void shutdown() {
      debug(String.format("Call ServerService::shutdown: serverEntry=%s", this.toString()));
      this.channel.shutdown();
    }

    @Override
    public String toString() {
      return String.format("{address=%s, qualifier=%s}", this.address, this.qualifier);
    }
  }

  private List<ServerEntry> serverEntries;

  /** Constructor when no services are found */
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

  /** Returns true if there are servers currently available */
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

  /** Removes all servers from the Server Entries list */
  public void removeServers() {
    for (ServerEntry server : this.serverEntries) {
      debug(String.format("Call TupleSpacesService::removeServers: server=%s", server.toString()));
      server.shutdown();
      this.serverEntries.remove(server);
    }
  }

  /** Perform shutdown logic */
  public void shutdown() {
    for (ServerEntry server : this.serverEntries) {
      debug(String.format("Call TupleSpacesService::shutdown: server=%s", server.toString()));
      server.shutdown();
    }
  }

  /**
   * TupleSpaces 'put' gRPC wrapper.
   *
   * @param tuple String of the tuple we wish to save to the server
   * @param server Server where we which to invoke the RPC
   * @param observer TupleSpacesStreamObserver for async stub
   */
  public void put(
      String tuple, ServerEntry server, TupleSpacesStreamObserver<PutResponse> observer) {
    debug(
        String.format(
            "Call TupleSpacesService::put: tuple=%s, server=%s, observer=%s",
            tuple, server, observer));
    server.stub.put(PutRequest.newBuilder().setNewTuple(tuple).build(), observer);
  }

  /**
   * TupleSpaces 'read' gRPC wrapper.
   *
   * @param searchPattern A regex pattern (or simply a string) that matches the tuple we want to
   *     read from the given server.
   * @param server Server where we which to invoke the RPC
   * @param observer TupleSpacesStreamObserver for async stub
   */
  public void read(
      String searchPattern, ServerEntry server, TupleSpacesStreamObserver<ReadResponse> observer) {
    debug(
        String.format(
            "Call TuplesSpacesService::read: searchPattern=%s, server=%s, observer=%s",
            searchPattern, server, observer));
    server.stub.read(ReadRequest.newBuilder().setSearchPattern(searchPattern).build(), observer);
  }

  /**
   * TupleSpaces 'take' gRPC wrapper.
   *
   * @param searchPattern A regex pattern (or simply a string) that matches the tuple we want to
   *     read from the given server.
   * @param server Server where we which to invoke the RPC
   * @param observer TupleSpacesStreamObserver for async stub
   */
  public void take(
      String searchPattern, ServerEntry server, TupleSpacesStreamObserver<TakeResponse> observer) {
    debug(
        String.format(
            "Call TuplesSpacesService::take: searchPattern=%s, server=%s, observer=%s",
            searchPattern, server, observer));
    server.stub.take(TakeRequest.newBuilder().setSearchPattern(searchPattern).build(), observer);
  }

  /**
   * TupleSpaces 'getTupleSpacesState' gRPC wrapper.
   *
   * @param server Server where we which to invoke the RPC
   * @param observer TupleSpacesStreamObserver for async stub
   */
  public void getTupleSpacesState(
      ServerEntry server, TupleSpacesStreamObserver<getTupleSpacesStateResponse> observer) {
    debug(
        String.format(
            "Call TuplesSpacesService::getTupleSpacesState: server=%s, observer=%s",
            server, observer));
    server.stub.getTupleSpacesState(getTupleSpacesStateRequest.getDefaultInstance(), observer);
  }
}
