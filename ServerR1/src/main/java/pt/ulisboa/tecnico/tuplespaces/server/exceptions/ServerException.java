package pt.ulisboa.tecnico.tuplespaces.server.exceptions;

public class ServerException extends Exception {
  public ServerException(String err) {
    super(String.format(err));
  }
}
