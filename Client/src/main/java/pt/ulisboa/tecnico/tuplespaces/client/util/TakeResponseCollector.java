package pt.ulisboa.tecnico.tuplespaces.client.util;

import java.util.List;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;

import java.util.ArrayList;

public class TakeResponseCollector {
    List<List<String>> responses;
    List<Exception> exceptions;
    
    public TakeResponseCollector() {
        this.responses = new ArrayList<>();
        this.exceptions = new ArrayList<>();
    }

    public synchronized void saveException(Exception e) {
        debug(String.format("Call TakeResponseCollector::saveException: message=%s", e.getMessage()));
        exceptions.add(e);
        notifyAll();
    }

    public synchronized List<Exception> getExceptions() {
        debug("Call TakeResponseCollector::getExceptions");
        return exceptions;
    }

    public synchronized void saveResponse(List<String> response) {
        debug(String.format("Call TakeResponseCollector::saveResponse: response=%s", response));
        responses.add(response);
        notifyAll();
    }

    public synchronized List<List<String>> getResponses() {
        debug("Call TakeResponseCollector::getResponses");
        return responses;
    }

    public synchronized void waitAllResponses(int n) {
        debug(String.format("Call TakeResponseCollector::waitAllResponses: responseNumber=%d", n));
        while ( (responses.size() + exceptions.size()) < n) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}