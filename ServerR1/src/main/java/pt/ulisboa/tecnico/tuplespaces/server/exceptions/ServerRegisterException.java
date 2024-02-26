package pt.ulisboa.tecnico.tuplespaces.server.exceptions;

public class ServerRegisterException extends ServerException {
  public ServerRegisterException(String service, String qualifier, String address, String err) {
    super(String.format("Failed server %s %s %s registration. %s", service, qualifier, address, err));
  }
}
