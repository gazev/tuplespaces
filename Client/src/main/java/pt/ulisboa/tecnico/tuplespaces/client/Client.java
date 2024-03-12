package pt.ulisboa.tecnico.tuplespaces.client;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;
import static pt.ulisboa.tecnico.tuplespaces.client.CommandProcessor.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import pt.ulisboa.tecnico.tuplespaces.client.exceptions.InvalidArgumentException;
import pt.ulisboa.tecnico.tuplespaces.client.exceptions.InvalidCommandException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.NameServerService;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.TupleSpacesStreamObserver;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.TupleSpacesTakeStreamObserver;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.TuplesSpacesService;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.TuplesSpacesService.ServerEntry;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerNoServersException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerRPCFailureException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.TakeTooManyCollisionsException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.TupleSpacesServiceException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.TupleSpacesServiceRPCFailureException;
import pt.ulisboa.tecnico.tuplespaces.client.util.ClientResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;
import pt.ulisboa.tecnico.tuplespaces.client.util.TakeResponseCollector;

public class Client {
  public static final int RPC_RETRIES = 0; // we assume servers aren't faulty and network is good
  public static final int BACKOFF_RETRIES = 6;
  public static final int SLOT_DURATION = 1; // 1 second

  public static final String PHASE_1 = "take phase 1";
  public static final String PHASE_2 = "take phase 2";
  public static final String PHASE_1_RELEASE = "take phase 1 release";

  private final Integer id;
  private final String serviceName;
  private final String serviceQualifier;
  private final TuplesSpacesService tupleSpacesService;
  private final NameServerService nameServerService;
  private OrderedDelayer delayer;

  public Client(
      String serviceName,
      String serviceQualifier,
      TuplesSpacesService tupleSpacesService,
      NameServerService nameServerService) {
    this.id = randomId();
    this.serviceName = serviceName;
    this.serviceQualifier = serviceQualifier;
    this.tupleSpacesService = tupleSpacesService;
    this.nameServerService = nameServerService;
    setDelayer(tupleSpacesService.getServers().size());
  }

  /** Perform shutdown logic */
  public void shutdown() {
    debug("Call Client::shutdown");
    nameServerService.shutdown();
    tupleSpacesService.shutdown();
  }

  /** Set delayer for current number of active servers */
  public void setDelayer(int nrServers) {
    this.delayer = new OrderedDelayer(nrServers);
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
        setDelayer(tupleSpacesService.getServers().size());
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
      System.err.printf(
          "[ERROR] Invalid argument %s for command %s. Error: %s\n", args, command, e.getMessage());
      return;
    } catch (TakeTooManyCollisionsException e) {
      System.err.printf(
          "[ERROR] Couldn't acquire a tuple after %d attempts, aborting take operation",
          BACKOFF_RETRIES);
      return;
    } catch (TupleSpacesServiceException e) {
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
          command, args, RPC_RETRIES + 1);
      return;
    }

    System.out.println("OK");
    if (!result.isEmpty()) {
      System.out.println(result);
    }
    System.out.println(); // print new line after result because thats what the examples do
  }

  private String execute(String command, String args)
      throws InvalidCommandException,
          InvalidArgumentException,
          TupleSpacesServiceException,
          TakeTooManyCollisionsException {
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

  /**
   * Simply calls TupleSpacesService put and waits on all responses, @see TupleSpacesService.put()
   */
  private String put(String tuple) throws TupleSpacesServiceException, InvalidArgumentException {
    if (!isValidTupleOrSearchPattern(tuple)) throw new InvalidArgumentException("Invalid tuple");

    ClientResponseCollector collector = new ClientResponseCollector();
    for (Integer index : delayer) {
      ServerEntry server = tupleSpacesService.getServer(index);
      tupleSpacesService.put(
          tuple,
          server,
          new TupleSpacesStreamObserver<>(
              PUT, server.getAddress(), server.getQualifier(), collector));
    }

    collector.waitAllResponses(tupleSpacesService.getServers().size());
    if (!collector.getExceptions().isEmpty()) {
      throw new TupleSpacesServiceException(collector.getExceptions().get(0).getMessage());
    }

    return ""; // put doesn't print any information
  }

  /**
   * Simply calls TupleSpacesService read and waits for first response, @see
   * TupleSpacesService.read()
   */
  private String read(String searchPattern)
      throws InvalidArgumentException, TupleSpacesServiceException {
    if (!isValidTupleOrSearchPattern(searchPattern))
      throw new InvalidArgumentException("Invalid search pattern");

    ClientResponseCollector collector = new ClientResponseCollector();
    for (Integer id : delayer) {
      ServerEntry server = tupleSpacesService.getServer(id);
      tupleSpacesService.read(
          searchPattern,
          server,
          new TupleSpacesStreamObserver<>(
              READ, server.getAddress(), server.getQualifier(), collector));
    }

    collector.waitAllResponses(1);
    if (!collector.getExceptions().isEmpty()) {
      throw new TupleSpacesServiceException(collector.getExceptions().get(0).getMessage());
    }

    return collector.getResponses().get(0);
  }

  /** Perform 2 step XuLiskov take operation */
  private String take(String searchPattern)
      throws TupleSpacesServiceRPCFailureException,
          InvalidArgumentException,
          TakeTooManyCollisionsException {
    if (!isValidTupleOrSearchPattern(searchPattern))
      throw new InvalidArgumentException("Invalid search pattern");

    String takenTuple = null; // tuple chosen for second phase
    int retries = 0;
    while (retries < BACKOFF_RETRIES) {
      // initialize phase 1
      TakeResponseCollector collectorFirstPhase = new TakeResponseCollector(3);
      for (Integer id : delayer) {
        ServerEntry server = tupleSpacesService.getServer(id);
        tupleSpacesService.takePhase1(
            searchPattern,
            this.id,
            server,
            new TupleSpacesTakeStreamObserver<>(
                PHASE_1, server.getAddress(), server.getQualifier(), collectorFirstPhase));
      }

      debug("Waiting on 1st phase responses");
      collectorFirstPhase.waitAllResponses();
      if (!collectorFirstPhase.getExceptions().isEmpty()) {
        throw new TupleSpacesServiceRPCFailureException(
            collectorFirstPhase.getExceptions().get(0).getMessage());
      }

      // if we chose a tuple in phase 1 we can move to phase 2
      List<String> res = getResponsesIntersection(collectorFirstPhase.getResponses());
      if (!res.isEmpty()) {
        takenTuple = res.get(0);
        break;
      }

      // phase 1 release if unable to acquire a tuple
      TakeResponseCollector collectorRelease = new TakeResponseCollector(3);
      for (Integer id : delayer) {
        ServerEntry server = tupleSpacesService.getServer(id);
        tupleSpacesService.takePhase1Release(
            this.id,
            server,
            new TupleSpacesTakeStreamObserver<>(
                PHASE_1_RELEASE, server.getAddress(), server.getQualifier(), collectorRelease));
      }

      debug("Waiting on 1st phase release responses");
      collectorRelease.waitAllResponses();
      if (!collectorRelease.getExceptions().isEmpty()) {
        throw new TupleSpacesServiceRPCFailureException(
            collectorRelease.getExceptions().get(0).getMessage());
      }

      retries++;
      int backoff_slots = new Random().nextInt((int) (Math.pow(2, retries)));
      debug(
          String.format(
              "Exponential backoff time slots: %d, attempt number %s", backoff_slots, retries));
      setDelay(0, backoff_slots * SLOT_DURATION);
      setDelay(1, backoff_slots * SLOT_DURATION);
      setDelay(2, backoff_slots * SLOT_DURATION);
    }

    resetDelays();
    if (takenTuple == null) {
      throw new TakeTooManyCollisionsException();
    }

    // phase 2
    debug("Selected tuple in 1st phase: " + takenTuple);
    TakeResponseCollector collectorSecondPhase = new TakeResponseCollector(3);
    for (Integer id : delayer) {
      ServerEntry server = tupleSpacesService.getServer(id);
      tupleSpacesService.takePhase2(
          takenTuple,
          this.id,
          server,
          new TupleSpacesTakeStreamObserver<>(
              PHASE_2, server.getAddress(), server.getQualifier(), collectorSecondPhase));
    }

    debug("Waiting on 2nd phase responses");
    collectorSecondPhase.waitAllResponses();
    if (!collectorSecondPhase.getExceptions().isEmpty()) {
      throw new TupleSpacesServiceRPCFailureException(
          collectorSecondPhase.getExceptions().get(0).getMessage());
    }

    return takenTuple;
  }

  /**
   * Simply TupleSpacesService getTupleSpacesState on server with specified qualifier, @see
   * TupleSpacesService.getTupleSpacesState()
   */
  private String getTupleSpacesState(String serviceQualifier)
      throws InvalidArgumentException, TupleSpacesServiceException {

    ServerEntry server = tupleSpacesService.getServer(serviceQualifier);
    if (server == null)
      throw new InvalidArgumentException(
          String.format("No servers found for qualifier %s", serviceQualifier));

    ClientResponseCollector collector = new ClientResponseCollector();
    tupleSpacesService.getTupleSpacesState(
        server,
        new TupleSpacesStreamObserver<>(
            GET_TUPLE_SPACES_STATE, server.getAddress(), server.getQualifier(), collector));

    collector.waitAllResponses(1);
    if (!collector.getExceptions().isEmpty()) {
      throw new TupleSpacesServiceException(collector.getExceptions().get(0).getMessage());
    }

    return collector.getResponses().get(0);
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

  /**
   * Sets delay for server with given qualifier
   *
   * @param qualifier server to set the delay
   * @param delay delay in seconds
   */
  public void setDelay(int qualifier, int delay) {
    delayer.setDelay(qualifier, delay);
  }

  public void resetDelays() {
    delayer.resetDelays();
  }

  /**
   * Generate a random client ID
   *
   * @return random int
   */
  private Integer randomId() {
    UUID uuid = UUID.randomUUID();
    long mostSignificantBits = uuid.getMostSignificantBits();
    return (int) (mostSignificantBits & Integer.MAX_VALUE);
  }

  /**
   * Returns list of elements that intersect all given Lists
   *
   * @param responses List of lists with the common elements we want to determine
   * @return List of all common elements
   */
  private List<String> getResponsesIntersection(List<List<String>> responses) {
    List<String> intersection = new ArrayList<>(responses.get(0));
    for (List<String> list : responses) {
      intersection.retainAll(list);
    }
    return intersection;
  }
}
