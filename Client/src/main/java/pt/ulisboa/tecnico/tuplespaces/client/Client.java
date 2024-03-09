package pt.ulisboa.tecnico.tuplespaces.client;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;
import static pt.ulisboa.tecnico.tuplespaces.client.CommandProcessor.*;

import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.client.exceptions.InvalidArgumentException;
import pt.ulisboa.tecnico.tuplespaces.client.exceptions.InvalidCommandException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.NameServerService;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.TupleSpacesStreamObserver;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.TuplesSpacesService;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerNoServersException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerRPCFailureException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.TupleSpacesServiceException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.TupleSpacesServiceRPCFailureException;
import pt.ulisboa.tecnico.tuplespaces.client.util.ClientResponseCollector;

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
    debug("Call Client::shutdown");
    nameServerService.shutdown();
    tupleSpacesService.shutdown();
  }

  /** Remote invocation of TupleSpaces procedures entry point */
  public void executeTupleSpacesCommand(String command, String args, int retries) {
    debug(
        String.format(
            "Call Client::executeTupleSpacesCommand: command=%s, args=%s, retries=%d",
            command, args, retries));
    // if no current servers, lookup in name server
    if (!tupleSpacesService.hasServers()) {
      List<NameServerService.ServiceEntry> newServerEntries;
      try {
        newServerEntries = nameServerService.lookup(serviceName, serviceQualifier);
        tupleSpacesService.setServers(newServerEntries);
      } catch (NameServerRPCFailureException e) {
        System.err.printf(
            "[ERROR] Failed communicating with name server. Error: %s\n", e.getMessage());
        debug(String.format("Name server address: %s", nameServerService.getAddress()));
        return;
      } catch (NameServerNoServersException e) {
        System.err.printf("[WARN] No servers available. Error: %s\n", e.getMessage());
        debug(String.format("Name server address: %s", nameServerService.getAddress()));
        return;
      }
    }

    String result = "";
    try {
      result = execute(command, args);
    } catch (InvalidCommandException e) {
      System.err.printf("[ERROR] Invalid command %s. Error: %s\n", command, e.getMessage());
      return;
    } catch (InvalidArgumentException e) {
      System.err.printf("[ERROR] Invalid argument %s for command %s. Error: %s\n", args, command, e.getMessage());
      return;
    } catch (TupleSpacesServiceException e) {
      // TODO this might not be used in second phase
      System.err.printf("[ERROR] Failed %s RPC. Error: %s\n", command, e.getMessage());
      tupleSpacesService.removeServers(); // remove all servers
      if (retries != 0) {
        System.err.println(
                "[WARN] Assuming all servers are shutdown (specification doesn't consider faulty servers)...");
        System.err.println("[WARN] Retrying with new servers");
        executeTupleSpacesCommand(
                command, args, retries - 1); // retry the operation with new lookup
        return;
      }
      System.err.printf(
              "[ERROR] Couldn't complete %s procedure with arguments %s after %d attempts, procedure aborted\n",
              command, args, rpcRetry + 1);
      return;
    }

    System.out.println("OK");
    if (!result.isEmpty()) {
      System.out.println(result);
    }
    System.out.println(); // print new line after result because thats what the examples do
  }

  private String execute(String command, String args)
      throws InvalidCommandException, InvalidArgumentException, TupleSpacesServiceException {
    switch (command) {
      case PUT:
        return put(args);
      case READ:
        return read(args);
      case TAKE:
        return take(args);
      case GET_TUPLE_SPACES_STATE:
        return getTupleSpacesState(args);
      default:
        throw new InvalidCommandException("Unknown command");
    }
  }

  /** Simply calls TupleSpacesService put, @see TupleSpacesService.put() */
  private String put(String tuple)
      throws TupleSpacesServiceRPCFailureException, InvalidArgumentException {
    if (!isValidTupleOrSearchPattern(tuple)) throw new InvalidArgumentException("Invalid tuple");
    ClientResponseCollector collector = new ClientResponseCollector();
    for (TuplesSpacesService.ServerEntry server : tupleSpacesService.getServers()) {
      tupleSpacesService.put(tuple, server, new TupleSpacesStreamObserver<>("PUT", server.getAddress(), server.getQualifier(), collector));
    }
    collector.waitAllResponses(tupleSpacesService.getServers().size());
    return "";
  }

  /** Simply calls TupleSpacesService read, @see TupleSpacesService.read() */
  private String read(String searchPattern)
      throws TupleSpacesServiceRPCFailureException, InvalidArgumentException {
    if (!isValidTupleOrSearchPattern(searchPattern)) throw new InvalidArgumentException("Invalid search pattern");
    ClientResponseCollector collector = new ClientResponseCollector();
    for (TuplesSpacesService.ServerEntry server : tupleSpacesService.getServers()) {
      tupleSpacesService.read(searchPattern, server, new TupleSpacesStreamObserver<>("READ", server.getAddress(), server.getQualifier(), collector));
    }
    collector.waitAllResponses(tupleSpacesService.getServers().size());
    return collector.getResponses().get(0);
  }

  /** Simply calls TupleSpacesService take, @see TupleSpacesService.take() */
  private String take(String searchPattern)
      throws TupleSpacesServiceRPCFailureException, InvalidArgumentException {
    if (!isValidTupleOrSearchPattern(searchPattern)) throw new InvalidArgumentException("Invalid search pattern");

    return tupleSpacesService.take(searchPattern);
  }

  /**
   * Simply calls TupleSpacesService getTupleSpacesState, @see
   * TupleSpacesService.getTupleSpacesState()
   */
  private String getTupleSpacesState(String serviceQualifier)
      throws TupleSpacesServiceRPCFailureException {
    return tupleSpacesService.getTupleSpacesState(serviceQualifier);
  }

  /**
   * Returns true if given string is a valid Tuple or Search Pattern
   *
   * @param s tuple or search pattern to be validated
   * @return True if given `s` is valid
   */
  private boolean isValidTupleOrSearchPattern(String s) {
    return s.startsWith("<") && s.endsWith(">");
  }
}
