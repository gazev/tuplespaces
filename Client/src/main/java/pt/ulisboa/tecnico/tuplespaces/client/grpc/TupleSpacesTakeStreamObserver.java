package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import static pt.ulisboa.tecnico.tuplespaces.client.Client.PHASE_1;
import static pt.ulisboa.tecnico.tuplespaces.client.Client.PHASE_1_RELEASE;
import static pt.ulisboa.tecnico.tuplespaces.client.Client.PHASE_2;
import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.TupleSpacesServiceRPCFailureException;
import pt.ulisboa.tecnico.tuplespaces.client.util.TakeResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.util.TakeResponseCollector.TakeResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.*;

public class TupleSpacesTakeStreamObserver<R> implements StreamObserver<R> {
  private final String phase;
  private final String serverAddr;
  private final String serverQual;
  private final TakeResponseCollector collector;

  public TupleSpacesTakeStreamObserver(
      String phase, String serverAddr, String serverQual, TakeResponseCollector collector) {
    this.phase = phase;
    this.serverAddr = serverAddr;
    this.serverQual = serverQual;
    this.collector = collector;
  }

  @Override
  public void onNext(R response) {
    if (response instanceof TakePhase1Response && phase.equals(PHASE_1)) {
      debug(
          String.format(
              "TupleSpacesTakeStreamObserver::onNext serverAddr=%s, serverQual=%s: TakePhase1Response reservedTuples=%s",
              serverAddr, serverQual, ((TakePhase1Response) response).getReservedTuplesList()));
      collector.saveResponse(
          new TakeResponse(serverQual, ((TakePhase1Response) response).getReservedTuplesList()));
    } else if (response instanceof TakePhase2Response && phase.equals(PHASE_2)) {
      // does nothing, just print debug
      debug(
          String.format(
              "TupleSpacesTakeStreamObserver::onNext serverAddr=%s, serverQual=%s: TakePhase2Response",
              serverAddr, serverQual));
    } else if (response instanceof TakePhase1ReleaseResponse && phase.equals(PHASE_1_RELEASE)) {
      // does nothing, just print debug
      debug(
          String.format(
              "TupleSpacesTakeStreamObserver::onNext serverAddr=%s, serverQual=%s: TakePhase1ReleaseResponse",
              serverAddr, serverQual));
    } else {
      collector.saveException(
          new TupleSpacesServiceRPCFailureException(
              String.format(
                  "From server %s %s, got unknown response type for %s",
                  serverAddr, serverQual, phase)));
    }
    collector.taskDone();
  }

  @Override
  public void onError(Throwable t) {
    debug(
        String.format(
            "TupleSpacesStreamObserver::onError serverAddr=%s, serverQual=%s: t=%s",
            serverAddr, serverQual, t));

    collector.saveException(
        new TupleSpacesServiceRPCFailureException(
            String.format(
                "From server %s %s, got error for procedure %s. Error: %s",
                serverAddr, serverQual, phase, t.getMessage())));

    collector.taskDone();
  }

  @Override
  public void onCompleted() {
    debug(
        String.format(
            "TupleSpacesStreamObserver::onCompleted serverAddr=%s, serverQual=%s",
            serverAddr, serverQual));
  }
}
