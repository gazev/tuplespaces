package pt.ulisboa.tecnico.tuplespaces.client.util;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;

import java.util.ArrayList;
import java.util.List;

public class ClientResponseCollector {
  List<String> responses;
  List<Exception> exceptions;

  public ClientResponseCollector() {
    this.responses = new ArrayList<>();
    this.exceptions = new ArrayList<>();
  }

  public ClientResponseCollector(List<String> responses) {
    this.responses = responses;
  }

  public synchronized void saveException(Exception e) {
    exceptions.add(e);
    notifyAll();
  }

  public synchronized List<Exception> getExceptions() {
    return exceptions;
  }

  public synchronized void saveResponse(String response) {
    responses.add(response);
    notifyAll();
  }

  public synchronized List<String> getResponses() {
    return responses;
  }

  public synchronized void waitAllResponses(int n) {
    debug(String.format("ClientResponseCollector::waitAllResponses: responseNumber=%d", n));
    while ((responses.size() + exceptions.size()) < n) {
      try {
        wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
