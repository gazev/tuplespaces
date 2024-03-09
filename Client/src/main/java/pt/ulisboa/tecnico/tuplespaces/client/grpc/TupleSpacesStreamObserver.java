package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;
import static pt.ulisboa.tecnico.tuplespaces.client.CommandProcessor.*;
import static pt.ulisboa.tecnico.tuplespaces.client.CommandProcessor.GET_TUPLE_SPACES_STATE;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.TupleSpacesServiceRPCFailureException;
import pt.ulisboa.tecnico.tuplespaces.client.util.ClientResponseCollector;

public class TupleSpacesStreamObserver<R> implements StreamObserver<R> {
  private final ClientResponseCollector collector;
  private final String serverAddr;
  private final String serverQual;
  private final String procedureName;

  public TupleSpacesStreamObserver(
      String procedureName,
      String serverAddr,
      String serverQual,
      ClientResponseCollector collector) {
    this.procedureName = procedureName;
    this.serverAddr = serverAddr;
    this.serverQual = serverQual;
    this.collector = collector;
  }

  @Override
  public void onNext(R response) {
    debug(
        String.format(
            "TupleSpacesStreamObserver::onNext procedureName=%s, serverAddr=%s, serverQual=%s",
            procedureName, serverAddr, serverQual));
    String responseRepr = "";
    if (response instanceof ReadResponse && procedureName.equals(READ)) {
      responseRepr = ((ReadResponse) response).getResult();
    } else if (response instanceof TakeResponse && procedureName.equals(TAKE)) {
      responseRepr = ((TakeResponse) response).getResult();
    } else if (response instanceof PutResponse && procedureName.equals(PUT)) {
      // response is empty
    } else if (response instanceof getTupleSpacesStateResponse
        && procedureName.equals(GET_TUPLE_SPACES_STATE))
      responseRepr = ((getTupleSpacesStateResponse) response).getTupleList().toString();
    else {
      collector.saveException(
          new TupleSpacesServiceRPCFailureException(
              String.format(
                  "From server %s %s, got unknown response type for procedure %s",
                  serverAddr, serverQual, procedureName)));
    }
    collector.saveResponse(responseRepr);
  }

  @Override
  public void onError(Throwable t) {
    debug(
        String.format(
            "TupleSpacesStreamObserver::onError procedureName=%s, serverAddr=%s, serverQual=%s: t=%s",
            procedureName, serverAddr, serverQual, t));
    collector.saveException(
        new TupleSpacesServiceRPCFailureException(
            String.format(
                "From server %s %s, got error for procedure %s. Error: %s",
                serverAddr, serverQual, procedureName, t.getMessage())));
  }

  @Override
  public void onCompleted() {
    debug(
        String.format(
            "TupleSpacesStreamObserver::onCompleted procedureName=%s, serverAddr=%s, serverQual=%s",
            procedureName, serverAddr, serverQual));
  }
}
