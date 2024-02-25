package pt.ulisboa.tecnico.tuplespaces.server.exceptions;

public class ServerUnregisterException extends ServerException {
    public ServerUnregisterException(String service, String address) {
        super("ServerUnregisterException", String.format("Failed server %s %s unregistration", service, address));
    }
}
