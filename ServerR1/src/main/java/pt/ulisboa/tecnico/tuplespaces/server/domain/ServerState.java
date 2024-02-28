package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidInputSearchPatternException;
import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidInputTupleStringException;

public class ServerState {
  private static final String BGN_TUPLE = "<";
  private static final String END_TUPLE = ">";

  private final List<String> tuples;

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

    for (String t : this.tuples) {
      if (t.matches(pattern)) {
        return t;
      }
    }

    return null; // remove this when blocking done
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
      this.tuples.add(tuple);
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

  /**
   * Remove and read a tuple from the TupleSpaces that matches the given pattern.
   *
   * @param pattern to be matched
   * @return desired tuple
   * @throws InvalidInputSearchPatternException if given pattern is invalid
   */
  public String take(String pattern) throws InvalidInputSearchPatternException {
    if (isInvalidTuple(pattern)) {
      throw new InvalidInputSearchPatternException(pattern);
    }

    synchronized (this) {
      String tuple = getMatchingTuple(pattern);
      this.tuples.remove(tuple);
      return tuple;
    }
  }

  /**
   * Get a list of all tuples in the TupleSpaces.
   *
   * @return List of all tuples.
   */
  public synchronized List<String> getTupleSpacesState() {
    return this.tuples;
  }
}
