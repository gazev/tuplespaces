package pt.ulisboa.tecnico.tuplespaces.client;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;
import static pt.ulisboa.tecnico.tuplespaces.client.CommandProcessor.*;

import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.NameServerService;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.TuplesSpacesService;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerNoServersException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerRPCFailureException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.TupleSpacesServiceRPCFailureException;

public class Client {
  public static final int rpcRetry = 1;
  private final String serviceName;
  private final String serviceQualifier;
  private final TuplesSpacesService tupleSpacesService;
  private final NameServerService nameServerService;

  public Client(
      String serviceName,
      String serviceQualifier,
      TuplesSpacesService tupleSpacesService,
      NameServerService nameServerService) {
    this.serviceName = serviceName;
    this.serviceQualifier = serviceQualifier;
    this.tupleSpacesService = tupleSpacesService;
    this.nameServerService = nameServerService;
  }

  /** Perform shutdown logic */
  public void shutdown() {
    debug("Call Client.shutdown()");
    nameServerService.shutdown();
    tupleSpacesService.shutdown();
  }

  /** Remote invocation of TupleSpaces procedures entry point */
  public void invoke_remote_command(String command, String args, int retries) {
    debug(String.format("Call Client.invoke_remote_command(): command=%s, args=%s", command, args));
    // if no current servers, lookup in name server
    if (!tupleSpacesService.hasServers()) {
      List<NameServerService.ServiceEntry> newServerEntries;
      try {
        newServerEntries = nameServerService.lookup(serviceName, serviceQualifier);
        tupleSpacesService.setServer(newServerEntries);
      } catch (NameServerRPCFailureException e) {
        System.err.printf(
            "[ERROR] Failed communicating with name server. Error: %s\n", e.getMessage());
        return;
      } catch (NameServerNoServersException e) {
        System.err.println("[WARN] " + e.getMessage());
        return;
      }
    }

    String result = "";
    try {
      switch (command) {
        case PUT:
          put(args);
          break;
        case READ:
          result = read(args);
          break;
        case TAKE:
          result = take(args);
          break;
        case GET_TUPLE_SPACES_STATE:
          result = getTupleSpacesState(args);
          break;
        default:
          System.err.printf("Unknown command %s", command);
          return;
      }
    } catch (TupleSpacesServiceRPCFailureException e) {
      // TODO this might not be used in second phase
      System.err.printf(
          "[WARN] Failed procedure %s call on %s. Error: %s\n",
          command, tupleSpacesService.getServer().getAddress(), e.getMessage());
      tupleSpacesService.removeCurrentServer(); // remove server assumed to be shutdown or faulty
      if (retries != 0) {
        System.err.println("[WARN] Retrying with new servers");
        invoke_remote_command(command, args, retries - 1); // retry the operation with new lookup
        return;
      }
      System.err.printf(
          "[ERROR] Couldn't complete %s procedure after %d attempts, procedure aborted\n",
          command, rpcRetry + 1);
      return;
    }

    System.out.println("OK");
    if (!result.isEmpty()) {
      System.out.println(result);
    }
    System.out.println(); // print new line after result because thats what the examples do
  }

  /** Simply calls TupleSpacesService put, @see TupleSpacesService.put() */
  private void put(String tuple) throws TupleSpacesServiceRPCFailureException {
    tupleSpacesService.put(tuple);
  }

  /** Simply calls TupleSpacesService read, @see TupleSpacesService.read() */
  private String read(String searchPattern) throws TupleSpacesServiceRPCFailureException {
    return tupleSpacesService.read(searchPattern);
  }

  /** Simply calls TupleSpacesService take, @see TupleSpacesService.take() */
  private String take(String searchPattern) throws TupleSpacesServiceRPCFailureException {
    return tupleSpacesService.take(searchPattern);
  }

  /**
   * Simply calls TupleSpacesService getTupleSpacesState, @see
   * TupleSpacesService.getTupleSpacesState()
   */
  private String getTupleSpacesState(String serviceQualifier)
      throws TupleSpacesServiceRPCFailureException {
    return tupleSpacesService.getTupleSpacesState();
  }
}
