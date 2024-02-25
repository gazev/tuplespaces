package pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions;

public class InvalidInputSearchPatternException extends InvalidInputException {
  public InvalidInputSearchPatternException(String s) {
    super(String.format("Invalid search pattern '%s'", s));
  }
}
