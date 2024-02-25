package pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions;

import javax.naming.Name;

public class NameServerException extends Exception {
    public NameServerException(String s) {
        super("NameServerException: " + s);
    }
}

