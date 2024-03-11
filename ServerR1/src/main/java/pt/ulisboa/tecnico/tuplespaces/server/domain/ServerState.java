package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov;
import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidClient;
import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidInputException;
import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidInputSearchPatternException;
import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidInputTupleStringException;

public class ServerState {
  private static final String BGN_TUPLE = "<";
  private static final String END_TUPLE = ">";

  class Tuple {
    private final String tuple;
    private boolean locked;
    private Integer heldClientId;

    public Tuple(String tuple) {
      this.tuple = tuple;
      this.locked = false;
    }

    public String getTuple() {
      return tuple;
    }

    public Integer getHeldClientId() {
      return heldClientId;
    }

    public boolean isUnlocked() {
      return !locked;
    }

    public boolean isLocked() {
      return locked;
    }

    public void lock(Integer clientId) {
      locked = true;
      heldClientId = clientId;
    }

    public void unlock() {
      locked = false;
      heldClientId = null;
    }
  }

  private final List<Tuple> tuples;

  public ServerState() {
    this.tuples = new ArrayList<>();
  }

  /**
   * Determine if given tuple or search pattern is Invalid
   *
   * @param tuple tuple or search pattern to be tested
   * @return true if given tuple or search pattern is invalid
   */
  public static boolean isInvalidTuple(String tuple) {
    return !tuple.startsWith(BGN_TUPLE) || !tuple.endsWith(END_TUPLE);
  }

  /**
   * Get a tuple from the TupleSpace matching the given pattern.
   *
   * @param pattern search pattern
   * @return tuple matching the search pattern
   * @throws InvalidInputSearchPatternException if given search pattern is invalid
   */
  private String getMatchingTuple(String pattern) throws InvalidInputSearchPatternException {
    if (isInvalidTuple(pattern)) {
      throw new InvalidInputSearchPatternException(pattern);
    }

    synchronized (this) {
      while (true) {
        for (Tuple t : this.tuples) {
          if (t.getTuple().matches(pattern)) {
            return t.getTuple();
          }
        }
        try {
          wait();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  /**
   * Get a list of unlocked tuples from the TupleSpace matching the given pattern.
   *
   * @param pattern search pattern
   * @param clientId clientId requesting the list which will lock the retrieved tuples
   * @return list of tuples strings
   * @throws InvalidInputSearchPatternException if invalid search pattern is provided
   */
  private List<String> getUnlockedMatchingTuples(String pattern, Integer clientId)
      throws InvalidInputSearchPatternException {
    if (isInvalidTuple(pattern)) {
      throw new InvalidInputSearchPatternException(pattern);
    }

    synchronized (this) {
      return tuples.stream()
          .filter(t -> t.isUnlocked() && t.getTuple().matches(pattern))
          .peek(t -> t.lock(clientId))
          .map(Tuple::getTuple)
          .collect(Collectors.toList());
    }
  }

  /**
   * Unlock all tuples locked by client with clientId
   *
   * @param clientId with all tuples to be unlocked
   */
  private synchronized void unlockClientTuples(Integer clientId) {
    tuples.stream()
        .filter(t -> t.isLocked() && t.getHeldClientId().equals(clientId))
        .forEach(Tuple::unlock);
  }

  /**
   * Removes given tuple from the TupleSpace request by client with clientId
   *
   * @param tupleStr to be removed
   * @param clientId requesting client ID
   */
  private synchronized void removeTuple(String tupleStr, Integer clientId) throws InvalidClient {
    Tuple tuple = null;
    for (Tuple t : tuples) {
      if (t.isLocked() && t.getHeldClientId().equals(clientId) && t.getTuple().equals(tupleStr)) {
        tuple = t;
        break;
      }
    }

    if (tuple == null) {
      throw new InvalidClient(
          String.format("Client %s has no access to tuple %s", clientId, tuple));
    }

    tuples.remove(tuple);
  }

  /**
   * Put given tuple in the TupleSpaces.
   *
   * @param tuple new tuple to be added.
   * @throws InvalidInputTupleStringException if given tuple is invalid
   */
  public void put(String tuple) throws InvalidInputTupleStringException {
    if (isInvalidTuple(tuple)) {
      throw new InvalidInputTupleStringException(tuple);
    }

    synchronized (this) {
      this.tuples.add(new Tuple(tuple));
      notifyAll();
    }
  }

  /**
   * Read a tuple from the TupleSpaces matching the given pattern.
   *
   * @param pattern to be matched
   * @return desired tuple
   * @throws InvalidInputSearchPatternException if given pattern is invalid
   */
  public String read(String pattern) throws InvalidInputSearchPatternException {
    if (isInvalidTuple(pattern)) {
      throw new InvalidInputSearchPatternException(pattern);
    }

    synchronized (this) {
      return getMatchingTuple(pattern);
    }
  }

  public List<String> takePhase1(String pattern, Integer clientId)
      throws InvalidInputSearchPatternException {
    if (isInvalidTuple(pattern)) {
      throw new InvalidInputSearchPatternException(pattern);
    }

    return getUnlockedMatchingTuples(pattern, clientId);
  }

  public void takePhase1Release(Integer clientId) {
    unlockClientTuples(clientId);
  }

  public void takePhase2(String tupleString, Integer clientId) throws InvalidClient {
    removeTuple(tupleString, clientId);
  }

  /**
   * Get a list of all tuples in the TupleSpaces.
   *
   * @return List of all tuples.
   */
  public synchronized List<String> getTupleSpacesState() {
    return tuples.stream()
        .map(Tuple::getTuple)
        .collect(Collectors.toList()); // return copy of tuples list
  }
}
