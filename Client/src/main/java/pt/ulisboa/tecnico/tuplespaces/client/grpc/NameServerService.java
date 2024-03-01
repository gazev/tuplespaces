package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerNoServersException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerRPCFailureException;
import pt.ulisboa.tecnico.tuplespaces.nameserver.contract.NameServerGrpc;
import pt.ulisboa.tecnico.tuplespaces.nameserver.contract.NameServerOuterClass;

public class NameServerService {
  private final String address; // name server address
  private ManagedChannel channel;
  private NameServerGrpc.NameServerBlockingStub stub;

  public NameServerService(String nsAddress) {
    this.address = nsAddress;

    setup();
  }

  /** Create channel and stub for name server. */
  private void setup() {
    debug("Call NameServerService.connect()");
    this.channel = ManagedChannelBuilder.forTarget(this.address).usePlaintext().build();
    this.stub = NameServerGrpc.newBlockingStub(this.channel);
  }

  /** Perform name server shutdown logic. */
  public void shutdown() {
    debug("Call NameServerService.shutdown()");
    this.channel.shutdown();
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
            "Call NameServerService.lookup(): serviceName=%s, qualifier=%s",
            serviceName, qualifier));
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
