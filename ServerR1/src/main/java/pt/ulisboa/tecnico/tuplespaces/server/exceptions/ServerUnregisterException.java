package pt.ulisboa.tecnico.tuplespaces.server.exceptions;

public class ServerUnregisterException extends ServerException {
  public ServerUnregisterException(String service, String address) {
    super(String.format("Failed server %s %s unregistration", service, address));
  }
}
