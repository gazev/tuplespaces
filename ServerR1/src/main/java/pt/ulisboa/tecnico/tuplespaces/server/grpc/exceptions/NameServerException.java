package pt.ulisboa.tecnico.tuplespaces.server.grpc.exceptions;

public class NameServerException extends Exception {
  public NameServerException(String err) {
    super(String.format(err));
  }
}
