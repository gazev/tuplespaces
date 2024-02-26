package pt.ulisboa.tecnico.tuplespaces.server.grpc.exceptions;

public class NameServerRPCFailureException extends NameServerException {
  public NameServerRPCFailureException(String procedure, String err) {
    super(String.format("Failed %s RPC. Error: %s", procedure, err));
  }
}
