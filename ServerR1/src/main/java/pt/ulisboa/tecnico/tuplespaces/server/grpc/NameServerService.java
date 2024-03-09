package pt.ulisboa.tecnico.tuplespaces.server.grpc;

import static pt.ulisboa.tecnico.tuplespaces.server.ServerMain.debug;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.tuplespaces.nameserver.contract.NameServerGrpc;
import pt.ulisboa.tecnico.tuplespaces.nameserver.contract.NameServerOuterClass;
import pt.ulisboa.tecnico.tuplespaces.server.grpc.exceptions.NameServerRPCFailureException;


/** NameServerService class encapsulates the gRPC interface of the NameServer for a TupleSpaces Server */
public class NameServerService {
  private final String address;
  private ManagedChannel channel;
  private NameServerGrpc.NameServerBlockingStub stub;

  public NameServerService(String nsAddress) {
    this.address = nsAddress;

    setup();
  }

   /** Create channel and stub for name server. */
  private void setup() {
    debug("Call NameServerService::connect: No arguments");
    this.channel = ManagedChannelBuilder.forTarget(this.address).usePlaintext().build();
    this.stub = NameServerGrpc.newBlockingStub(this.channel);
  }

  /** Perform name server shutdown logic */
  public void shutdown() {
    debug("Call NameServerService::shutdown: No arguments");
    if (this.channel != null) this.channel.shutdown();
  }

  /**
   * NameServerService 'register' gRPC command wrapper.
   *
   * @param serviceName String representing the service that is being serving
   * @param qualifier   String representing the server's qualifier
   * @param address     String representing the server's address
   * @throws NameServerRPCFailureException on RPC failure or incapability of registering the server on the name server
   */
  public void register(String serviceName, String qualifier, String address)
      throws NameServerRPCFailureException {
    debug(
        String.format(
            "Call NameServerService::register: serviceName=%s, qualifier=%s, address=%s",
            serviceName, qualifier, address));
    try {
      this.stub.register(
          NameServerOuterClass.RegisterRequest.newBuilder()
              .setServiceName(serviceName)
              .setQualifier(qualifier)
              .setAddress(address)
              .build());
    } catch (StatusRuntimeException e) {
      throw new NameServerRPCFailureException("Register", e.getStatus().getDescription());
    }
  }

  /**
   * NameServerService 'delete' gRPC wrapper.
   *
   * @param serviceName String representing the service that is being served
   * @param address     String representing the server's address
   * @throws NameServerRPCFailureException on RPC failure or incapability of deleting the server in the name server
   */
  public void delete(String serviceName, String address) throws NameServerRPCFailureException {
    debug(
            String.format(
                    "Call NameServerService::delete: serviceName=%s,  address=%s",
                    serviceName, address));

    try {
      this.stub.delete(
          NameServerOuterClass.DeleteRequest.newBuilder()
              .setServiceName(serviceName)
              .setAddress(address)
              .build());
    } catch (StatusRuntimeException e) {
      throw new NameServerRPCFailureException("Delete", e.getStatus().getDescription());
    }
  }
}
