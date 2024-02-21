package pt.ulisboa.tecnico.tuplespaces.server.exceptions;

public class InvalidTupleException extends Exception {
    public InvalidTupleException(String s) {
        super("Invalid tuple " + s);
    }
}
