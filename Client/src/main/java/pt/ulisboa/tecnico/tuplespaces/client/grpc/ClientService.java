package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import pt.ulisboa.tecnico.tuplespaces.*;

public class ClientService {

  /*TODO: The gRPC client-side logic should be here.
        This should include a method that builds a channel and stub,
        as well as individual methods for each remote operation of this service. */

  public static ClientService newClientService() {
    return new ClientService();
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
   
}
