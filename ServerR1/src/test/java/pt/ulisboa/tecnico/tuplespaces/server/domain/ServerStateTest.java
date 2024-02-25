package pt.ulisboa.tecnico.tuplespaces.server.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.ulisboa.tecnico.tuplespaces.server.Server;
import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidInputException;
import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidInputSearchPatternException;
import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidInputTupleStringException;

class ServerStateTest {
  @Test
  void invalidTupleTest() {
    assertTrue(ServerState.isInvalidTuple("invalid"));
  }

  @Test
  void validTupleTest() {
    assertFalse(ServerState.isInvalidTuple("<sd,turno,vaga1>"));
  }

  @Test
  void invalidTupleTestStart() {
    assertTrue(ServerState.isInvalidTuple("<sd,turno,vaga1"));
  }

  @Test
  void invalidTupleTestEnd() {
    assertTrue(ServerState.isInvalidTuple("sd,turno,vaga1>"));
  }

  @Test
  void validTupleTestEsoteric() {
    assertFalse(ServerState.isInvalidTuple("<true,false,bool>"));
  }

  @Test
  void validTupleTestBig() {
    assertFalse(ServerState.isInvalidTuple("<sd,vaga,false,teo,turno2,turma3>"));
  }

  @Test
  void putTupleAndReadTest() {
    ServerState state = new ServerState();
    try {
      state.put("<sd,vaga,turno1>");
      assertEquals(state.read("<sd,vaga,turno1>"), "<sd,vaga,turno1>");
    } catch (InvalidInputException e) {
      fail("Failed with exception: " + e.getMessage());
    }
  }

  @Test
  void readTupleWithRegex() {
    ServerState state = new ServerState();
    try {
      state.put("<sd,vaga,turno1>");
      state.put("<sd,vaga,turno2>");
      String tuple = state.read("<sd,vaga,[^,]+>");
      assertTrue(tuple.equals("<sd,vaga,turno1>") || tuple.equals("<sd,vaga,turno2>"));
    } catch (InvalidInputException e) {
      fail("Failed with exception: " + e.getMessage());
    }
  }

  @Test
  void takeTuple() {
    ServerState state = new ServerState();
    try {
      state.put("<sd,vaga,turno1>");
      String tuple = state.take("<sd,vaga,turno1>");
      assertEquals(tuple, "<sd,vaga,turno1>");
    } catch (InvalidInputException e) {
      fail("Failed with exception: " + e.getMessage());
    }
  }

  @Test
  void takeRemoveTest() {
    ServerState state = new ServerState();
    try {
      state.put("<sd,vaga,turno1>");
      state.put("<sd,vaga,turno2>");
      String tuple = state.take("<sd,vaga,turno1>");
      assertEquals(tuple, "<sd,vaga,turno1>");
      assertEquals(state.getTupleSpacesState(), new ArrayList<>(List.of("<sd,vaga,turno2>")));
    } catch (InvalidInputException e)  {
      fail("Failed with exception: " + e.getMessage());
    }
  }

  @Test
  void takeRemoveRegexTest() {
    ServerState state = new ServerState();
    try {
      state.put("<sd,vaga,turno1>");
      state.put("<sd,vaga,turno2>");
      String tuple = state.take("<sd,vaga,[^,]+>");
      assertEquals(tuple, "<sd,vaga,turno1>");
      assertEquals(state.getTupleSpacesState(), new ArrayList<>(List.of("<sd,vaga,turno2>")));
    } catch (InvalidInputException e)  {
      fail("Failed with exception: " + e.getMessage());
    }
  }

  @Test
  void getAllTuplesTest() {
    ServerState state = new ServerState();
    try {
      state.put("<sd,vaga,turno1>");
      state.put("<sd,vaga,turno2>");
      state.put("<es,vaga,turno1>");
      assertEquals(
          state.getTupleSpacesState(),
          new ArrayList<>(
              Arrays.asList("<sd,vaga,turno1>", "<sd,vaga,turno2>", "<es,vaga,turno1>")));
    } catch (InvalidInputTupleStringException e) {
      fail("Failed with exception: " + e.getMessage());
    }
  }

  @Test
  void invalidInputPutTest() {
    ServerState state = new ServerState();
    try {
      state.put("invalidtuple");
      fail("Didn't throw expected InvalidInput");
    } catch (InvalidInputException e) {
      // all good if here
    }
  }

  @Test
  void invalidInputReadTest() {
    ServerState state = new ServerState();
    try {
      state.read("invalidtuple");
      fail("Didn't throw expected InvalidInput");
    } catch (InvalidInputException e) {
      // all good if here
    }
  }

  @Test
  void invalidInputTakeTest() {
    ServerState state = new ServerState();
    try {
      state.take("invalidtuple");
      fail("Didn't throw expected InvalidInput");
    } catch (InvalidInputException e) {
      // all good if here
    }
  }

  @Test
  void emptyTupleSpace() {
    ServerState state = new ServerState();
    assertEquals(state.getTupleSpacesState(), new ArrayList<>(List.of()));
  }
}
