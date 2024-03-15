package pt.ulisboa.tecnico.tuplespaces.client.util;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;
import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.serviceName;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TakeResponseCollector {
  public static class TakeResponse {
    private final String serverQual;
    private final List<String> tuplesList;

    public TakeResponse(String serverQual, List<String> response) {
      this.serverQual = serverQual;
      this.tuplesList = response;
    }

    public String getServerQual() {
      return serverQual;
    }

    public List<String> getTuplesList() {
      return tuplesList;
    }

    @Override
    public String toString() {
      return String.format("{serverQual=%s, tuplesList=%s}", serverQual, tuplesList);
    }
  }

  List<TakeResponse> responses;
  List<Exception> exceptions;
  int taskCount;

  public TakeResponseCollector() {
    this.responses = new ArrayList<>();
    this.exceptions = new ArrayList<>();
  }

  public synchronized List<TakeResponse> getResponses() {
    return responses;
  }

  public synchronized void setTaskCount(int n) {
    taskCount = n;
  }

  public synchronized List<Exception> getExceptions() {
    return exceptions;
  }

  public void removeUnlockedServerResponses() {
    exceptions = new ArrayList<>();
    responses = responses.stream().filter(t -> !t.getTuplesList().isEmpty()).collect(Collectors.toList());
  }

  public synchronized void taskDone() {
    debug(String.format("TakeResponseCollector::taskDone tuplesList=%s", responses));
    taskCount--;
    notifyAll();
  }

  public synchronized void saveResponse(TakeResponse response) {
    responses.add(response);
  }

  public synchronized void saveException(Exception e) {
    exceptions.add(e);
  }

  public synchronized void waitResponses() {
    debug(String.format("TakeResponseCollector::waitResponses: taskCount=%d", taskCount));
    while (taskCount > 0) {
      try {
        wait();
      } catch (InterruptedException e) {
        debug(String.format("InterruptedException: %s", e.getMessage()));
        throw new RuntimeException(e);
      }
    }
  }
}
