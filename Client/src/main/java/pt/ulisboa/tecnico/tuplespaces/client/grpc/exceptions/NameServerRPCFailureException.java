package pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions;

public class NameServerRPCFailureException extends NameServerException {
    public NameServerRPCFailureException(String procedure, String err) {
        super("Failed " + procedure + " RPC: " + err);
    }
}
