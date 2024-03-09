package pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions;

public class TupleSpacesServiceRPCFailureException extends TupleSpacesServiceException {
  public TupleSpacesServiceRPCFailureException(String err) {
    super(err);
  }
}
