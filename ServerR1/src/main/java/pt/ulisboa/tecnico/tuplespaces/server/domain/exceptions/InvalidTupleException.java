package pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions;

public class InvalidTupleException extends Exception {
  public InvalidTupleException(String s) {
    super("Invalid tuple " + s);
  }
}
