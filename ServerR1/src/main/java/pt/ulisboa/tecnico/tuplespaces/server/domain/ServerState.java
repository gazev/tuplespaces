package pt.ulisboa.tecnico.tuplespaces.server.domain;

import static pt.ulisboa.tecnico.tuplespaces.server.ServerMain.debug;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidInputSearchPatternException;
import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidInputTupleStringException;

public class ServerState {
  private static final String BGN_TUPLE = "<";
  private static final String END_TUPLE = ">";

  class PendingTake {
    private String searchPattern;

    public PendingTake(String pattern) {
      searchPattern = pattern;
    }

    public String getSearchPattern() {
      return searchPattern;
    }
  }

  private final List<String> tuples; // tuples in the tuplespace

  private Integer state = 1;
  private final Lock stateLock = new ReentrantLock();
  private final Condition stateChange = stateLock.newCondition();

  private final List<PendingTake> pendingTakes = new LinkedList<>(); // FIFO

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
   * Put given tuple in the TupleSpaces.
   *
   * @param tuple new tuple to be added.
   * @throws InvalidInputTupleStringException if given tuple is invalid
   */
  public void put(String tuple, Integer seqNumber) throws InvalidInputTupleStringException {
    if (isInvalidTuple(tuple)) {
      throw new InvalidInputTupleStringException(tuple);
    }

    // lock until it's this operation time to be executed
    stateLock.lock();
    while (!seqNumber.equals(state)) {
      try {
        debug(String.format("put SN %d, state %d - Out of order, waiting", seqNumber, state));
        stateChange.await();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    synchronized (this) {
      this.tuples.add(tuple);
      notifyAll(); // unlock reads
      state += 1;
    }

    // wake up oldest take waiting for this tuple
    for (PendingTake pendingOperation : pendingTakes) {
      synchronized (pendingOperation) {
        if (tuple.matches(pendingOperation.getSearchPattern())) {
          debug(String.format("put SN %d - Notified pending take for %s", seqNumber, pendingOperation.searchPattern));
          pendingOperation.notify(); // notify take waitin
          break;
        }
      }
    }

    stateChange.signalAll();
    stateLock.unlock();
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
      while (true) {
        for (String t : this.tuples) {
          if (t.matches(pattern)) {
            return t;
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

  public String take(String pattern, Integer seqNumber) throws InvalidInputSearchPatternException {
    if (isInvalidTuple(pattern)) {
      throw new InvalidInputSearchPatternException(pattern);
    }

    // lock until it's this operation time to be executed
    stateLock.lock();
    while (!seqNumber.equals(state)) {
      try {
        debug(String.format("take SN %d, state %d - Out of order, waiting", seqNumber, state));
        stateChange.await();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    state += 1;

    // first attempt at getting tuple
    synchronized (this) {
      for (String t : tuples) {
        if (t.matches(pattern)) {
          tuples.remove(t);
          stateChange.signalAll();
          stateLock.unlock();
          return t;
        }
      }
    }

    debug(String.format("take SN %d - No tuple found", seqNumber));

    // doesn't exist, block waiting on put
    PendingTake pendingOperation = new PendingTake(pattern);
    pendingTakes.add(pendingOperation);
    // if we get here and tuple still doesn't exist, we wait
    synchronized (pendingOperation) {
      try {
        stateChange.signalAll();
        stateLock.unlock();
        debug(
            String.format(
                "take SN %d - Waiting for %s",
                seqNumber, pendingOperation.getSearchPattern()));
        pendingOperation.wait(); // unblocked by put operation
        pendingTakes.remove(pendingOperation);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    // after unlock, tuple will exist
    synchronized (this) {
      for (String t : tuples) {
        if (t.matches(pattern)) {
          tuples.remove(t);
          return t;
        }
      }
    }

    // never reached, if it's reached then there might be a bug
    throw new RuntimeException(
        "Reach end of ServerState::take function, this is unexpected behaviour and shouldn't happen");
  }

  /**
   * Get a list of all tuples in the TupleSpaces.
   *
   * @return List of all tuples.
   */
  public synchronized List<String> getTupleSpacesState() {
    return new ArrayList<>(tuples);
  }
}
