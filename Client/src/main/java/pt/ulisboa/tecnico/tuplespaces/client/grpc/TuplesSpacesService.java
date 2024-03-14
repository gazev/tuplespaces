package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.*;

/** TuplesSpacesService class encapsulates the gRPC interface of the TupleSpaces service client. */
public class TuplesSpacesService {

  /** ServerEntry class represents a gRPC server of the TupleSpaces network */
  public static class ServerEntry {
    public final String qualifier; // server qualifier
    public final String address; // server address
    public ManagedChannel channel;
    public TupleSpacesReplicaGrpc.TupleSpacesReplicaStub stub;

    public ServerEntry(String address, String qualifier) {
      this.address = address;
      this.qualifier = qualifier;

      setup();
    }

    /** Create channel and stub for given server */
    private void setup() {
      debug(String.format("ServerEntry::setup %s", this));
      this.channel = ManagedChannelBuilder.forTarget(this.address).usePlaintext().build();
      this.stub = TupleSpacesReplicaGrpc.newStub(this.channel);
    }

    public String getAddress() {
      return this.address;
    }

    public String getQualifier() {
      return this.qualifier;
    }

    /** Perform server shutdown logic */
    public void shutdown() {
      debug(String.format("ServerEntry::shutdown %s", this));
      this.channel.shutdown();
    }

    @Override
    public String toString() {
      return String.format("{address=%s, qualifier=%s}", this.address, this.qualifier);
    }
  }

  private List<ServerEntry> serverEntries = new ArrayList<>();

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
   * @param serviceEntries List of service entries retrieved from name server lookup procedure
   */
  public void setServers(List<NameServerService.ServiceEntry> serviceEntries) {
    debug("TupleSpacesService::setServers");
    for (NameServerService.ServiceEntry service : serviceEntries)
      this.serverEntries.add(new ServerEntry(service.getAddress(), service.getQualifier()));

    this.serverEntries.sort(Comparator.comparing(ServerEntry::getQualifier));
  }

  /**
   * Add a single server to the Server Entries list
   *
   * @param server ServerEntry object
   */
  public void addServer(NameServerService.ServiceEntry server) {
    debug(String.format("TupleSpacesService::addServer: serverEntry=%s", server.toString()));
    this.serverEntries.add(new ServerEntry(server.getAddress(), server.getQualifier()));
  }

  /**
   * Get a server from the Server Entries list by its qualifier
   *
   * @param qualifier Server qualifier
   * @return ServerEntry object
   */
  public ServerEntry getServer(String qualifier) {
    debug(String.format("TupleSpacesService::getServer: qualifier=%s", qualifier));
    for (ServerEntry server : this.serverEntries) {
      if (server.getQualifier().equals(qualifier)) {
        return server;
      }
    }
    return null;
  }

  /**
   * Get a server from the Server Entries by index
   *
   * @param index Server index (servers are alpha sorted)
   * @return Server at given index
   */
  public ServerEntry getServer(Integer index) {
    return serverEntries.get(index);
  }

  /**
   * Get all servers from the Server Entries list
   *
   * @return List of ServerEntry objects
   */
  public List<ServerEntry> getServers() {
    return this.serverEntries;
  }

  /** Returns true if there are servers currently available */
  public boolean hasServers() {
    return (!this.serverEntries.isEmpty());
  }

  /**
   * Removes one server from the Server Entries list
   *
   * @param qualifier Server qualifier
   */
  public void removeSingleServer(String qualifier) {
    ServerEntry server = getServer(qualifier);
    if (server != null) {
      server.shutdown();
      this.serverEntries.remove(server);
    }
  }

  /** Removes all servers from the Server Entries list */
  public void removeServers() {
    for (ServerEntry server : this.serverEntries) server.shutdown();

    serverEntries = new ArrayList<>();
  }

  /** Perform shutdown logic */
  public void shutdown() {
    debug("TupleSpacesService::shutdown");
    for (ServerEntry server : this.serverEntries) {
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
            "TupleSpacesService::put: tuple=%s, server=%s, observer=%s",
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
            "TuplesSpacesService::read: searchPattern=%s, server=%s, observer=%s",
            searchPattern, server, observer));
    server.stub.read(ReadRequest.newBuilder().setSearchPattern(searchPattern).build(), observer);
  }

  /**
   * TupleSpaces 'takePhase1' gRPC wrapper.
   *
   * @param searchPattern A regex pattern (or simply a string) that matches the tuple we want to
   *     read from the given server.
   * @param clientId Client identifier
   * @param server Server where we which to invoke the RPC
   * @param observer TupleSpacesStreamObserver for async stub
   */
  public void takePhase1(
      String searchPattern,
      Integer clientId,
      ServerEntry server,
      TupleSpacesTakeStreamObserver<TakePhase1Response> observer) {
    debug(
        String.format(
            "TuplesSpacesService::takePhase1: searchPattern=%s, server=%s, observer=%s",
            searchPattern, server, observer));
    server.stub.takePhase1(
        TakePhase1Request.newBuilder()
            .setSearchPattern(searchPattern)
            .setClientId(clientId)
            .build(),
        observer);
  }

  /**
   * TupleSpaces 'takePhase1Release' gRPC wrapper.
   *
   * @param clientId Client identifier
   * @param server Server where we which to invoke the RPC
   * @param observer TupleSpacesStreamObserver for async stub
   */
  public void takePhase1Release(
      Integer clientId,
      ServerEntry server,
      TupleSpacesTakeStreamObserver<TakePhase1ReleaseResponse> observer) {
    debug(
        String.format(
            "TuplesSpacesService::takePhase1Release: server=%s, observer=%s",
            server, observer));
    server.stub.takePhase1Release(
        TakePhase1ReleaseRequest.newBuilder().setClientId(clientId).build(), observer);
  }

  /**
   * TupleSpaces 'takePhase2' gRPC wrapper.
   *
   * @param clientId Client identifier
   * @param server Server where we which to invoke the RPC
   * @param observer TupleSpacesStreamObserver for async stub
   */
  public void takePhase2(
      String tuple,
      Integer clientId,
      ServerEntry server,
      TupleSpacesTakeStreamObserver<TakePhase2Response> observer) {
    debug(
        String.format(
            "TuplesSpacesService::takePhase2: server=%s, observer=%s", server, observer));
    server.stub.takePhase2(
        TakePhase2Request.newBuilder().setTuple(tuple).setClientId(clientId).build(), observer);
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
            "TuplesSpacesService::getTupleSpacesState: server=%s, observer=%s",
            server, observer));
    server.stub.getTupleSpacesState(getTupleSpacesStateRequest.getDefaultInstance(), observer);
  }
}
