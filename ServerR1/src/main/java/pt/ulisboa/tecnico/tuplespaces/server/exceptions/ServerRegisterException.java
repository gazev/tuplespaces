package pt.ulisboa.tecnico.tuplespaces.server.exceptions;

public class ServerRegisterException extends ServerException {
    public ServerRegisterException(String service, String qualifier, String address) {
        super("ServerRegisterException", String.format("Failed server %s %s %s registration", service, qualifier, address));
    }
}
