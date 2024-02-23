package pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions;

public class InvalidSearchPatternException extends Exception {
  public InvalidSearchPatternException(String s) {
    super("Invalid search pattern " + s);
  }
}
