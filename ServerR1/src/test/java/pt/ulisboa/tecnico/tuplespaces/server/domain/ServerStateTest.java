package pt.ulisboa.tecnico.tuplespaces.server.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidSearchPatternException;
import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidTupleException;

class ServerStateTest {
  @Test
  void putTupleAndReadTest() {
    ServerState state = new ServerState();
    try {
      state.put("<sd, vaga, turno1>");
      assertEquals(state.read("<sd, vaga, turno1>"), "<sd, vaga, turno1>");
    } catch (InvalidSearchPatternException | InvalidTupleException e) {
      fail("Failed with exception: " + e.getMessage());
    }
  }

  @Test
  void readTupleWithRegex() {
    ServerState state = new ServerState();
    try {
      state.put("<sd, vaga, turno1>");
      state.put("<sd, vaga, turno2>");
      String tuple = state.read("<sd, vaga, [^,]+>");
      assertTrue(tuple.equals("<sd, vaga, turno1>") || tuple.equals("<sd, vaga, turno2>"));
    } catch (InvalidSearchPatternException | InvalidTupleException e) {
      fail("Failed with exception: " + e.getMessage());
    }
  }

  @Test
  void takeTuple() {
    ServerState state = new ServerState();
    try {
      state.put("<sd, vaga, turno1>");
      String tuple = state.take("<sd, vaga, turno1>");
      assertEquals(tuple, "<sd, vaga, turno1>");
      assertEquals(state.read("<sd, vaga, turno1>"), null);
    } catch (InvalidTupleException | InvalidSearchPatternException e) {
      fail("Failed with exception: " + e.getMessage());
    }
  }

  @Test
  void getAllTuplesTest() {
    ServerState state = new ServerState();
    try {
      state.put("<sd, vaga, turno1>");
      state.put("<sd, vaga, turno2>");
      state.put("<es, vaga, turno1>");
      assertEquals(
          state.getTupleSpacesState(),
          new ArrayList<String>(
              Arrays.asList("<sd, vaga, turno1>", "<sd, vaga, turno2>", "<es, vaga, turno1>")));
    } catch (InvalidTupleException e) {
      fail("Failed with exception: " + e.getMessage());
    }
  }
}
