package pt.ulisboa.tecnico.tuplespaces.client;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;
import static pt.ulisboa.tecnico.tuplespaces.client.CommandProcessor.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.tuplespaces.client.exceptions.InvalidArgumentException;
import pt.ulisboa.tecnico.tuplespaces.client.exceptions.InvalidCommandException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.NameServerService;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.SequencerService;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.TupleSpacesStreamObserver;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.TupleSpacesTakeStreamObserver;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.TuplesSpacesService;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.TuplesSpacesService.ServerEntry;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerNoServersException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerRPCFailureException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.BackoffRetriesExceeded;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.TupleSpacesServiceException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.TupleSpacesServiceRPCFailureException;
import pt.ulisboa.tecnico.tuplespaces.client.util.ClientResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;
import pt.ulisboa.tecnico.tuplespaces.client.util.TakeResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.util.TakeResponseCollector.TakeResponse;

public class Client {
  public static final int RPC_RETRIES = 0; // we assume servers aren't faulty and network is good
  public static final int BACKOFF_RETRIES = 5;
  public static final int SLOT_DURATION = 1; // 1 second

  public static final String PHASE_1 = "take phase 1";
  public static final String PHASE_2 = "take phase 2";
  public static final String PHASE_1_RELEASE = "take phase 1 release";

  private final Integer id;
  private final String serviceName;
  private final String serviceQualifier;
  private final TuplesSpacesService tupleSpacesService;
  private final NameServerService nameServerService;
  private final SequencerService sequencerService;
  private OrderedDelayer delayer;

  public Client(
      String serviceName,
      String serviceQualifier,
      TuplesSpacesService tupleSpacesService,
      NameServerService nameServerService) {
    this.id = randomId();
    debug("Client ID: " + this.id);
    this.serviceName = serviceName;
    this.serviceQualifier = serviceQualifier;
    this.tupleSpacesService = tupleSpacesService;
    this.nameServerService = nameServerService;
    this.sequencerService = new SequencerService();
    setDelayer(3);
  }

  /** Perform shutdown logic */
  public void shutdown() {
    debug("Client::shutdown");
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
            "Client::executeTupleSpacesCommand: command=%s, args=%s, retries=%d",
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
    } catch (BackoffRetriesExceeded e) {
      System.err.printf(
          "[ERROR] Couldn't acquire a tuple after %d retries with backoff, aborting take operation\n",
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
          BackoffRetriesExceeded {
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

    ExecutorService executor = Executors.newSingleThreadExecutor();
    ClientResponseCollector collector = new ClientResponseCollector();
    executor.submit(
        () -> {
          for (Integer id : delayer) {
            ServerEntry server = tupleSpacesService.getServer(id);
            tupleSpacesService.read(
                searchPattern,
                server,
                new TupleSpacesStreamObserver<>(
                    READ, server.getAddress(), server.getQualifier(), collector));
          }
        });

    collector.waitAllResponses(1);
    if (!collector.getExceptions().isEmpty()) {
      executor.shutdown();
      throw new TupleSpacesServiceException(collector.getExceptions().get(0).getMessage());
    }

    executor.shutdown();
    return collector.getResponses().get(0);
  }

  /** Perform 2 step XuLiskov take operation */
  private String take(String searchPattern)
      throws TupleSpacesServiceException, InvalidArgumentException, BackoffRetriesExceeded {
    if (!isValidTupleOrSearchPattern(searchPattern))
      throw new InvalidArgumentException("Invalid search pattern");

    int retries = 0; // collision retry counter
    Set<String> lockedServers = new HashSet<>(); // servers already locked
    TakeResponseCollector collectorFirstPhase = new TakeResponseCollector();
    while (retries < BACKOFF_RETRIES) {
      // phase1 request
      takePhase1(searchPattern, lockedServers, collectorFirstPhase);

      // add locked servers to locked servers set
      for (TakeResponse r : collectorFirstPhase.getResponses()) {
        if (!r.getTuplesList().isEmpty()) lockedServers.add(r.getServerQual()); // locked server
      }

      // got minority, release and backoff
      if (lockedServers.size() <= 1) {
        takePhase1Release(lockedServers);
        retries++;
        int backoff_slots = new Random().nextInt((int) (Math.pow(2, retries))) + 1;
        try {
          debug(
              String.format(
                  "Client::take, backoff_slots = %d, retry = %d", backoff_slots, retries));
          Thread.sleep((long) backoff_slots * SLOT_DURATION * 1000);
        } catch (InterruptedException e) {
          debug(String.format("InterruptedException: %s", e.getMessage()));
          throw new RuntimeException(e);
        }
        lockedServers = new HashSet<>();
        collectorFirstPhase = new TakeResponseCollector();
        continue;
      }

      // got majority, but not all servers locked, repeat phase 1 only on remaining servers
      if (lockedServers.size() != 3) {
        // we apply a little delay because other clients can't instantly release the minority
        try {
          Thread.sleep(SLOT_DURATION * 500);
        } catch (InterruptedException e) {
          debug(String.format("InterruptedException: %s", e.getMessage()));
          throw new RuntimeException(e);
        }
        collectorFirstPhase.removeUnlockedServerResponses();
        debug("Retrying phase 1, no lock on all servers");
        continue;
      }

      // calculate intersection
      List<String> responseIntersection =
          getResponsesIntersection(
              collectorFirstPhase.getResponses().stream()
                  .map(TakeResponse::getTuplesList)
                  .collect(Collectors.toList()));

      // empty intersection, retry phase1
      if (responseIntersection.isEmpty()) {
        // we apply a little delay here because we don't want to spam the server
        try {
          Thread.sleep(SLOT_DURATION * 500);
        } catch (InterruptedException e) {
          debug(String.format("InterruptedException: %s", e.getMessage()));
          throw new RuntimeException(e);
        }
        debug("Retrying phase 1, empty intersection on all servers");
        lockedServers = new HashSet<>();
        collectorFirstPhase = new TakeResponseCollector();
        continue;
      }

      // tuple to be removed in phase 2
      String chosenTuple = responseIntersection.get(0);
      // phase2
      takePhase2(chosenTuple);
      return chosenTuple;
    }

    // in XuLiskov it is said the client should keep requesting, but since we are unaware of what are real world Linda's
    // requirements we simply set a limit, this is easily altered
    throw new BackoffRetriesExceeded(); // backoff limit exceeded, which can also indicate tuple doesn't exist
  }

  private void takePhase1(
      String searchPattern, Set<String> lockedServers, TakeResponseCollector collector)
      throws TupleSpacesServiceRPCFailureException {
    debug(String.format("Client::takePhase1: lockedServers=%s", lockedServers));

    collector.setTaskCount(3 - lockedServers.size());
    for (Integer id : delayer) {
      ServerEntry server = tupleSpacesService.getServer(id);
      if (lockedServers.contains(server.getQualifier())) continue; // skip servers already locked

      tupleSpacesService.takePhase1(
          searchPattern,
          this.id,
          server,
          new TupleSpacesTakeStreamObserver<>(
              PHASE_1, server.getAddress(), server.getQualifier(), collector));
    }

    debug(
        String.format(
            "Client::takePhase1, blocked waiting on #%d 1st phase responses",
            3 - lockedServers.size()));
    collector.waitResponses();
    if (!collector.getExceptions().isEmpty()) {
      throw new TupleSpacesServiceRPCFailureException(
          collector.getExceptions().get(0).getMessage());
    }
  }

  private void takePhase1Release(Set<String> lockedServers)
      throws TupleSpacesServiceRPCFailureException {
    debug(String.format("Client::takePhase1Release: lockedServers=%s", lockedServers));

    TakeResponseCollector collector = new TakeResponseCollector();
    collector.setTaskCount(lockedServers.size());
    for (Integer id : delayer) {
      ServerEntry server = tupleSpacesService.getServer(id);
      if (!lockedServers.contains(server.getQualifier()))
        continue; // dont send release to servers which client hasn't locked

      tupleSpacesService.takePhase1Release(
          this.id,
          server,
          new TupleSpacesTakeStreamObserver<>(
              PHASE_1_RELEASE, server.getAddress(), server.getQualifier(), collector));
    }

    debug(
        String.format(
            "Client::takePhase1, blocked waiting on #%d release responses", lockedServers.size()));
    collector.waitResponses();
    if (!collector.getExceptions().isEmpty()) {
      throw new TupleSpacesServiceRPCFailureException(
          collector.getExceptions().get(0).getMessage());
    }
  }

  private void takePhase2(String tuple) throws TupleSpacesServiceRPCFailureException {
    debug(String.format("Client::takePhase2: tuple=%s", tuple));

    TakeResponseCollector collector = new TakeResponseCollector();
    collector.setTaskCount(3);
    for (Integer id : delayer) {
      ServerEntry server = tupleSpacesService.getServer(id);
      tupleSpacesService.takePhase2(
          tuple,
          this.id,
          server,
          new TupleSpacesTakeStreamObserver<>(
              PHASE_2, server.getAddress(), server.getQualifier(), collector));
    }

    debug("Client::takePhase2, blocked waiting on #3 2nd phase responses");
    collector.waitResponses();
    if (!collector.getExceptions().isEmpty()) {
      throw new TupleSpacesServiceRPCFailureException(
          collector.getExceptions().get(0).getMessage());
    }
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

  private Integer getSequenceNumber() {
    Integer seq = null;
    try {
      seq = sequencerService.getSeqNumber();
    } catch (Exception e) {
      System.err.println("Failed to get sequence number");
      System.err.println(e.getMessage());
    }
    return seq;
  }
}
