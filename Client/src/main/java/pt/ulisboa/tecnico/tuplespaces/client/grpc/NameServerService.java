package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerNoServersException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerRPCFailureException;
import pt.ulisboa.tecnico.tuplespaces.nameserver.contract.NameServerGrpc;
import pt.ulisboa.tecnico.tuplespaces.nameserver.contract.NameServerOuterClass;

/**
 * NameServerService class encapsulates the gRPC interface of the NameServer for a TupleSpaces
 * Client
 */
public class NameServerService {
  private final String address; // name server address
  private ManagedChannel channel;
  private NameServerGrpc.NameServerBlockingStub stub;

  public NameServerService(String nsAddress) {
    this.address = nsAddress;

    setup();
  }

  public String getAddress() {
    return address;
  }

  /** Create channel and stub for name server. */
  private void setup() {
    debug(String.format("NameServerService::connect %s", this));
    this.channel = ManagedChannelBuilder.forTarget(this.address).usePlaintext().build();
    this.stub = NameServerGrpc.newBlockingStub(this.channel);
  }

  /** Perform name server shutdown logic. */
  public void shutdown() {
    debug(String.format("NameServerService::shutdown %s", this));
    this.channel.shutdown();
  }

  @Override
  public String toString() {
    return String.format("{address=%s}", address);
  }

  /**
   * Class meant to mirror a ServiceEntry protobuf message that we can expand on without relying on
   * protobuf generated classes
   */
  public static class ServiceEntry {
    private final String address; // service address
    private final String qualifier; // service qualifier (e.g "A", "B", "C")

    public ServiceEntry(String address, String qualifier) {
      this.address = address;
      this.qualifier = qualifier;
    }

    public String getAddress() {
      return address;
    }

    public String getQualifier() {
      return qualifier;
    }
  }

  /**
   * NameServer service 'lookup' gRPC wrapper.
   *
   * @param serviceName procedure ServiceName argument
   * @param qualifier procedure Qualifier argument
   * @return list of fetched ServiceEntries
   * @throws NameServerRPCFailureException on RPC failure
   * @throws NameServerNoServersException if name server returns an empty list
   */
  public List<ServiceEntry> lookup(String serviceName, String qualifier)
      throws NameServerRPCFailureException, NameServerNoServersException {
    debug(
        String.format(
            "NameServerService::lookup %s: serviceName=%s, qualifier=%s",
            this, serviceName, qualifier));
    NameServerOuterClass.LookupResponse response = null;
    try {
      response =
          this.stub.lookup(
              NameServerOuterClass.LookupRequest.newBuilder()
                  .setServiceName(serviceName)
                  .setQualifier(qualifier)
                  .build());
    } catch (StatusRuntimeException e) {
      debug(e.getMessage());
      throw new NameServerRPCFailureException("Lookup", e.getStatus().getDescription());
    }

    List<NameServerOuterClass.LookupResponse.ServiceEntry> serversEntries =
        response.getServiceEntriesList();

    // check if no service exists
    if (serversEntries.isEmpty()) {
      throw new NameServerNoServersException(serviceName, qualifier);
    }

    return serversEntries.stream()
        .map(entry -> new ServiceEntry(entry.getServiceAddress(), entry.getQualifier()))
        .collect(Collectors.toList());
  }
}
