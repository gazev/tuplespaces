package pt.ulisboa.tecnico.tuplespaces.client;

import static pt.ulisboa.tecnico.tuplespaces.client.CommandProcessor.*;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.NameServerService;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.TuplesSpacesService;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.TupleSpacesServiceRPCFailureException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerNoServersException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerRPCFailureException;

import java.util.List;

public class Client {
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
    nameServerService.shutdown();
    tupleSpacesService.shutdown();
  }

  /** Remote invocation of TupleSpaces procedures entry point */
  public void invoke_remote_command(String command, String args) {
    // if no current servers, lookup in name server
    if (!tupleSpacesService.hasServers()) {
      List<NameServerService.ServiceEntry> newServerEntries;
      try {
        newServerEntries = nameServerService.lookup(serviceName, serviceQualifier);
        tupleSpacesService.setServer(newServerEntries);
      } catch (NameServerRPCFailureException e) {
        System.err.printf("Failed communicating with name server. Error: %s\n", e.getMessage());
        return;
      } catch (NameServerNoServersException e) {
        System.err.println(e.getMessage());
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
      }
    } catch (TupleSpacesServiceRPCFailureException e) {
      // if remote procedure doesn't work, we assume the server has shut down or is faulty
      tupleSpacesService.removeCurrentServer();
      System.err.printf("Failed remote procedure call: Error %s\n", e.getMessage());
      return;
    }

    System.out.println("OK");
    if (!result.isEmpty()) {
      System.out.println(result);
    }
    System.out.println(); // print new line after result because thats what the examples do
  }

  /**
   * Simply calls TupleSpacesService put, @see TupleSpacesService.put()
   */
  public void put(String tuple) throws TupleSpacesServiceRPCFailureException {
    tupleSpacesService.put(tuple);
  }

  /**
   * Simply calls TupleSpacesService read, @see TupleSpacesService.read()
   */
  public String read(String searchPattern) throws TupleSpacesServiceRPCFailureException {
    return tupleSpacesService.read(searchPattern);
  }

  /**
   * Simply calls TupleSpacesService take, @see TupleSpacesService.take()
   */
  public String take(String searchPattern) throws TupleSpacesServiceRPCFailureException {
    return tupleSpacesService.take(searchPattern);
  }

  /**
   * Simply calls TupleSpacesService getTupleSpacesState, @see TupleSpacesService.getTupleSpacesState()
   */
  public String getTupleSpacesState(String serviceQualifier) throws TupleSpacesServiceRPCFailureException {
    return tupleSpacesService.getTupleSpacesState();
  }
}
