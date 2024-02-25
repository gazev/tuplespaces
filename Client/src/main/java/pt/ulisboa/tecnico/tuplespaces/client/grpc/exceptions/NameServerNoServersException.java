package pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions;

public class NameServerNoServersException extends NameServerException {
    public NameServerNoServersException(String service, String qualifier) {
        super("No servers available for service " + service + " and qualifier " + qualifier + ". Unable to continue...");
    }
}
