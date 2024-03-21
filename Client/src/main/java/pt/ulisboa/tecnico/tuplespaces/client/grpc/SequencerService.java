package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import static pt.ulisboa.tecnico.tuplespaces.client.ClientMain.debug;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.sequencer.contract.*;
import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass.*;

public class SequencerService {
    public final String address;
    public ManagedChannel channel;
    public SequencerGrpc.SequencerBlockingStub stub;

    public SequencerService() {
        this.address = "localhost:8080";

        setup();
    }

    private void setup() {
        debug("SequencerService::setup");
        this.channel = ManagedChannelBuilder.forTarget(this.address).usePlaintext().build();
        this.stub = SequencerGrpc.newBlockingStub(this.channel);
    }

    public String getAddress() {
        return this.address;
    }

    public void shutdown() {
        debug("SequencerService::shutdown");
        this.channel.shutdown();
    }

    public Integer getSeqNumber() {
        GetSeqNumberRequest request = GetSeqNumberRequest.newBuilder().build();
        GetSeqNumberResponse response = this.stub.getSeqNumber(request);
        return response.getSeqNumber();
    }
}
