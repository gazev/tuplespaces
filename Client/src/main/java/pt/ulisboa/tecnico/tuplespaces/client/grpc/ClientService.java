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

  /** ServerService class represents a gRPC server of the TupleSpaces service */
  public static class ServerService {
    public final String address;
    public final String qualifier;
    public ManagedChannel channel;
    public TupleSpacesGrpc.TupleSpacesBlockingStub stub;

    public ServerService(String address, String qualifier) {
      this.address = address;
      this.qualifier = qualifier;
    }

    /** Create channel and stub for given server */
    public void connect() {
      debug("Call ServerService.connect");
      this.channel = ManagedChannelBuilder.forTarget(this.address).usePlaintext().build();
      this.stub = TupleSpacesGrpc.newBlockingStub(this.channel);
    }

    /** Server shutdown logic */
    public void shutdown() {
      debug("Call ServerService.shutdown");
      channel.shutdown();
    }
  }

  private final ServerService
      server; // server we are talking to, only one now, TODO probably a list on 2nd delivery

  /**
   * @param serverEntries available servers fetched from name server
   */
  public ClientService(List<NameServerService.ServiceEntry> serverEntries) {
    this.server =
        new ServerService(
            serverEntries.get(0).getAddress(),
            serverEntries.get(0).getQualifier()); // TODO first delivery only 1 server
    this.server.connect();
  }

  /** Perform shutdown logic */
  public void shutdown() {
    debug("Call ClientService.shutdown");
    if (server != null) server.shutdown();
  }

  /**
   * 'put' gRPC wrapper.
   *
   * @param tuple put procedure argument
   * @throws ClientServiceRPCFailureException on RPC failure
   */
  public void put(String tuple) throws ClientServiceRPCFailureException {
    debug("Call ClientService.put: tuple=" + tuple);
    try {
      server.stub.put(TupleSpacesCentralized.PutRequest.newBuilder().setNewTuple(tuple).build());
    } catch (StatusRuntimeException e) {
      debug(e.getMessage());
      throw new ClientServiceRPCFailureException("Put", e.getStatus().getDescription());
    }
  }

  /**
   * 'read' gRPC wrapper.
   *
   * @param searchPattern read procedure argument
   * @return string of the operation result
   * @throws ClientServiceRPCFailureException on RPC failure
   */
  public String read(String searchPattern) throws ClientServiceRPCFailureException {
    debug("Call ClientService.read: searchPattern=" + searchPattern);
    TupleSpacesCentralized.ReadResponse response = null;
    try {
      response =
          server.stub.read(
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
   * 'take' gRPC wrapper.
   *
   * @param searchPattern take procedure argument
   * @return string of the operation result
   * @throws ClientServiceRPCFailureException on RPC failure
   */
  public String take(String searchPattern) throws ClientServiceRPCFailureException {
    debug("Call ClientService.take: searchPattern=" + searchPattern);
    TupleSpacesCentralized.TakeResponse response = null;
    try {
      response =
          server.stub.take(
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
   * 'getTupleSpacesState' gRPC wrapper.
   *
   * @return string of the operation result
   * @throws ClientServiceRPCFailureException on RPC failure
   */
  public String getTupleSpacesState() throws ClientServiceRPCFailureException {
    debug("Call ClientService.getTupleSpacesState");
    TupleSpacesCentralized.getTupleSpacesStateResponse response = null;
    try {
      response =
          server.stub.getTupleSpacesState(
              TupleSpacesCentralized.getTupleSpacesStateRequest.getDefaultInstance());
    } catch (StatusRuntimeException e) {
      debug(e.getStatus().getDescription());
      throw new ClientServiceRPCFailureException(
          "GetTupleSpacesState", e.getStatus().getDescription());
    }

    // return first result
    return response.getTupleList().toString();
  }
}
