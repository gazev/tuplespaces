package pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions;

public class ClientServiceRPCFailureException extends ClientServiceException {
    public ClientServiceRPCFailureException(String procedure, String err) {
        super("Failed " + procedure + " RPC: " + err);
    }
}
