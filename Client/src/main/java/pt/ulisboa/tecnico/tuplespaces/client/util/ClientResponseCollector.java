package pt.ulisboa.tecnico.tuplespaces.client.util;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;

import java.util.ArrayList;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.TupleSpacesServiceException;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.TupleSpacesServiceRPCFailureException;

public class ClientResponseCollector {
  List<String> responses = new ArrayList<>();
  List<TupleSpacesServiceException> exceptions = new ArrayList<>();

  public ClientResponseCollector() {}

  public ClientResponseCollector(List<String> responses) {
    this.responses = responses;
  }

  public synchronized void saveException(TupleSpacesServiceRPCFailureException e) {
    debug(String.format("Call ClientResponseCollector::saveException: message=%s", e.getMessage()));
    exceptions.add(e);
    notifyAll();
  }

  public synchronized void saveResponse(String response) {
    debug(String.format("Call ClientResponseCollector::saveResponse: response=%s", response));
    responses.add(response);
    notifyAll();
  }

  public synchronized List<String> getResponses() {
    debug("Call ClientResponseCollector::getResponses");
    return new ArrayList<>(responses);
  }

  public synchronized List<TupleSpacesServiceException> getExceptions() {
    return new ArrayList<>(exceptions);
  }

  public synchronized void waitAllResponses(int n) {
    debug(String.format("Call ClientResponseCollector::waitAllResponses: responseNumber=%d", n));
    while ((responses.size() + exceptions.size()) < n) {
      try {
        wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
        throw new RuntimeException();
      }
    }
  }
}
