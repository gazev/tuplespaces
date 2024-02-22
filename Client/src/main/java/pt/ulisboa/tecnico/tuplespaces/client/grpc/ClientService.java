package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import pt.ulisboa.tecnico.tuplespaces.client;

public class ClientService {

  /* The gRPC client-side logic should be here.
        This should include a method that builds a channel and stub,
        as well as individual methods for each remote operation of this service.
        
        Done: channel, stub, wrappers, getters, setters, constructors
        TODO: individual methods for each remote operation*/

  private String service_name;
  private String target;
  private ManagedChannel channel;
  private BlockingStub blockingStub;

  /**
   * Creates a new ClientService
   * @param target
   * @param service_name
   * @return
   */
  public static ClientService newClientService(String target, String service_name) {
    ClientService clientService = new ClientService();
    clientService.target = target;
    clientService.service_name = service_name;
    clientService.channel = newChannel(target);
    clientService.blockingStub = newBlockingStub(this.channel);
    return clientService;
  }

  /**
   * Gets the service name
   * @return
   */
  public String getServiceName() {
    return this.service_name;
  }

  /**
   * Gets the target
   * @return
   */
  public String getTarget() {
    return this.target;
  }

  /**
   * Sets the service name
   * @param service_name
   */
  public void setServiceName(String service_name) {
    this.service_name = service_name;
  }

  /**
   * Sets the target
   * @param target
   */
  public void setTarget(String target) {
    this.target = target;
  }

  /**
   * Creates a new channel to the target
   * @param target
   * @return
   */
  public static ManagedChannel newChannel(String target) {
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    return channel;
  }

  /**
   * Shuts down the channel
   * @param channel
   */
  public static void shutdownChannel(ManagedChannel channel) {
    channel.shutdownNow();
  }

  /**
   * Creates a new blocking stub
   * @param channel
   * @return
   */
  public static BlockingStub newBlockingStub(ManagedChannel channel) {
    BlockingStub blockingStub = TupleSpacesGrpc.newBlockingStub(channel);
    return blockingStub;
  }

  public static void put(String tuple) {
    return;
  }

  public static void read(String tuple) {
    return;
  }

  public static void take(String tuple) {
    return;
  }

  public static void getTupleSpacesState(String qualifier) {
    return;
  }

}
