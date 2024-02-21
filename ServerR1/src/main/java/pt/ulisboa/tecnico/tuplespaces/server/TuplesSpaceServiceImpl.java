package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.Status;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc.TupleSpacesImplBase;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import pt.ulisboa.tecnico.tuplespaces.server.exceptions.InvalidSearchPatternException;
import pt.ulisboa.tecnico.tuplespaces.server.exceptions.InvalidTupleException;

public class TuplesSpaceServiceImpl extends TupleSpacesImplBase {
    private final ServerState tuplesSpace = new ServerState();

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> streamObserver) {
        try {
            tuplesSpace.put(request.getNewTuple());
        } catch (InvalidTupleException e) {
            streamObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
            return;
        }

        streamObserver.onNext(PutResponse.getDefaultInstance());
        streamObserver.onCompleted();
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> streamObserver) {
        String readTuple;
        try {
            readTuple = tuplesSpace.read(request.getSearchPattern());
        } catch (InvalidSearchPatternException e) {
            streamObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
            return;
        }

        streamObserver.onNext(ReadResponse.newBuilder().setResult(readTuple).build());
        streamObserver.onCompleted();
    }

    @Override
    public void take(TakeRequest request, StreamObserver<TakeResponse> streamObserver) {
        String takenTuple;
        try {
            takenTuple = tuplesSpace.read(request.getSearchPattern());
        } catch (InvalidSearchPatternException e) {
            streamObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
            return;
        }

        streamObserver.onNext(TakeResponse.newBuilder().setResult(takenTuple).build());
        streamObserver.onCompleted();
    }

    @Override
    public void getTupleSpacesState(getTupleSpacesStateRequest request, StreamObserver<getTupleSpacesStateResponse> streamObserver) {
        getTupleSpacesStateResponse response = getTupleSpacesStateResponse.newBuilder().addAllTuple(tuplesSpace.getTupleSpacesState()).build();

        streamObserver.onNext(response);
        streamObserver.onCompleted();
    }
}
