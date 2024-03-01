package pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions;

public class TupleSpacesServiceRPCFailureException extends TupleSpacesServiceException {
  public TupleSpacesServiceRPCFailureException(String procedure, String err) {
    super(String.format("Failed %s RPC. Error: %s", procedure, err));
  }
}
