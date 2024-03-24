package pt.ulisboa.tecnico.tuplespaces.server;

import static pt.ulisboa.tecnico.tuplespaces.server.ServerMain.debug;

import io.grpc.Status;
import java.util.List;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc.*;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidInputSearchPatternException;
import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidInputTupleStringException;

public class TuplesSpaceServiceImpl extends TupleSpacesReplicaImplBase {
  private final ServerState tuplesSpace;

  public TuplesSpaceServiceImpl(ServerState state) {
    this.tuplesSpace = state;
  }

  @Override
  public void put(PutRequest request, StreamObserver<PutResponse> streamObserver) {
    try {
      tuplesSpace.put(request.getNewTuple(), request.getSeqNumber());
    } catch (InvalidInputTupleStringException e) {
      debug(e.getMessage());
      System.err.println("[ERROR] Got invalid tuple " + request.getNewTuple());
      streamObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
      return;
    }

    System.out.println("[INFO] " + String.format("Ran 'put' on %s", request.getNewTuple()));
    streamObserver.onNext(PutResponse.getDefaultInstance());
    streamObserver.onCompleted();
  }

  @Override
  public void read(ReadRequest request, StreamObserver<ReadResponse> streamObserver) {
    String readTuple;
    try {
      readTuple = tuplesSpace.read(request.getSearchPattern());
    } catch (InvalidInputSearchPatternException e) {
      debug(e.getMessage());
      System.err.println("[ERROR] Got invalid search pattern " + request.getSearchPattern());
      streamObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
      return;
    }

    System.out.println("[INFO] " + String.format("Ran 'read' on %s", request.getSearchPattern()));
    streamObserver.onNext(ReadResponse.newBuilder().setResult(readTuple).build());
    streamObserver.onCompleted();
  }

  @Override
  public void take(TakeRequest request, StreamObserver<TakeResponse> streamObserver) {
    String takenTuple;
    try {
      takenTuple = tuplesSpace.take(request.getSearchPattern(), request.getSeqNumber());
    } catch (InvalidInputSearchPatternException e) {
      debug(e.getMessage());
      System.err.println("[ERROR] Got invalid tuple " + request.getSearchPattern());
      streamObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
      return;
    }

    System.out.println("[INFO] " + String.format("Ran 'take' on %s", request.getSearchPattern()));
    streamObserver.onNext(TakeResponse.newBuilder().setResult(takenTuple).build());
    streamObserver.onCompleted();
  }

  @Override
  public void getTupleSpacesState(
      getTupleSpacesStateRequest request,
      StreamObserver<getTupleSpacesStateResponse> streamObserver) {
    getTupleSpacesStateResponse response =
        getTupleSpacesStateResponse
            .newBuilder()
            .addAllTuple(tuplesSpace.getTupleSpacesState())
            .build();

    System.out.println("[INFO] Ran 'getTupleSpaces'");
    streamObserver.onNext(response);
    streamObserver.onCompleted();
  }
}
