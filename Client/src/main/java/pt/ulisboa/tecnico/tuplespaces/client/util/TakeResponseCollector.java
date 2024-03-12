package pt.ulisboa.tecnico.tuplespaces.client.util;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;

import java.util.ArrayList;
import java.util.List;

public class TakeResponseCollector {
  List<List<String>> responses;
  List<Exception> exceptions;
  int minResponses;

  public TakeResponseCollector(int minResponses) {
    this.responses = new ArrayList<>();
    this.exceptions = new ArrayList<>();
    this.minResponses = minResponses;
  }

  public synchronized List<List<String>> getResponses() {
    return responses;
  }

  public synchronized List<Exception> getExceptions() {
    return exceptions;
  }

  public synchronized void taskDone() {
    minResponses--;
    notifyAll();
  }

  public synchronized void saveResponse(List<String> response) {
    responses.add(response);
  }

  public synchronized void saveException(Exception e) {
    exceptions.add(e);
  }

  public synchronized void waitAllResponses() {
    debug("Call TakeResponseCollector::waitAllResponses");
    while (minResponses > 0) {
      try {
        wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
