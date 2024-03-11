package pt.ulisboa.tecnico.tuplespaces.server;

import static pt.ulisboa.tecnico.tuplespaces.server.ServerMain.debug;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc.*;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.*;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import pt.ulisboa.tecnico.tuplespaces.server.domain.exceptions.InvalidClient;
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
      tuplesSpace.put(request.getNewTuple());
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
  public void takePhase1(
      TakePhase1Request request, StreamObserver<TakePhase1Response> streamObserver) {
    List<String> reservedTuples;
    try {
      reservedTuples = tuplesSpace.takePhase1(request.getSearchPattern(), request.getClientId());
    } catch (InvalidInputSearchPatternException e) {
      debug(e.getMessage());
      System.err.println("[ERROR] Got invalid search pattern " + request.getSearchPattern());
      streamObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
      return;
    }

    System.out.println(
        "[INFO] "
            + String.format(
                "Ran 'takePhase1' on %s for client %d",
                request.getSearchPattern(), request.getClientId()));
    streamObserver.onNext(
        TakePhase1Response.newBuilder().addAllReservedTuples(reservedTuples).build());
    streamObserver.onCompleted();
  }

  @Override
  public void takePhase1Release(
      TakePhase1ReleaseRequest request, StreamObserver<TakePhase1ReleaseResponse> streamObserver) {
    tuplesSpace.takePhase1Release(request.getClientId());

    System.out.println(
        "[INFO] " + String.format("Ran 'takePhase1Release' for client %d", request.getClientId()));
    streamObserver.onNext(TakePhase1ReleaseResponse.getDefaultInstance());
    streamObserver.onCompleted();
  }

  @Override
  public void takePhase2(
      TakePhase2Request request, StreamObserver<TakePhase2Response> streamObserver) {
    try {
      tuplesSpace.takePhase2(request.getTuple(), request.getClientId());
    } catch (InvalidClient e) {
      System.err.println("[ERROR] " + e.getMessage());
      streamObserver.onError(
          Status.PERMISSION_DENIED.withDescription(e.getMessage()).asRuntimeException());
    }

    System.out.println(
        "[INFO] "
            + String.format(
                "Ran 'takePhase2' on %s for client %d", request.getTuple(), request.getClientId()));
    streamObserver.onNext(TakePhase2Response.getDefaultInstance());
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
