package pt.ulisboa.tecnico.tuplespaces.server.domain;

import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidSearchPatternException;
import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidTupleException;

import java.util.ArrayList;
import java.util.List;

public class ServerState {
  private static final String BGN_TUPLE = ">";
  private static final String END_TUPLE = "<";

  private List<String> tuples;

  public ServerState() {
    this.tuples = new ArrayList<String>();
  }

  public boolean isValidTuple(String tuple) {
    return tuple.startsWith(BGN_TUPLE) && tuple.endsWith(END_TUPLE);
  }

  public void put(String tuple) throws InvalidTupleException {
    if (!isValidTuple(tuple)) {
      throw new InvalidTupleException(tuple);
    }

    synchronized (this) {
      tuples.add(tuple);
    }
  }

  private String getMatchingTuple(String pattern) throws InvalidSearchPatternException {
    if (!isValidTuple(pattern)) {
      throw new InvalidSearchPatternException(pattern);
    }

    synchronized (this) {
      for (String tuple : this.tuples) {
        if (tuple.matches(pattern)) {
          return tuple;
        }
      }
    }
    return null;
  }

  public String read(String pattern) throws InvalidSearchPatternException {
    if (!isValidTuple(pattern)) {
      throw new InvalidSearchPatternException(pattern);
    }

    synchronized (this) {
      return getMatchingTuple(pattern);
    }
  }

  public String take(String pattern) throws InvalidSearchPatternException {
    if (!isValidTuple(pattern)) {
      throw new InvalidSearchPatternException(pattern);
    }

    // TODO
    return null;
  }

  public List<String> getTupleSpacesState() {
    // TODO
    return null;
  }
}
