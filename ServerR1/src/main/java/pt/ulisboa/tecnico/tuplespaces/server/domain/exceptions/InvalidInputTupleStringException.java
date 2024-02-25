package pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions;

public class InvalidInputTupleStringException extends InvalidInputException {
  public InvalidInputTupleStringException(String s) {
    super(String.format("Invalid tuple '%s'", s));
  }
}
