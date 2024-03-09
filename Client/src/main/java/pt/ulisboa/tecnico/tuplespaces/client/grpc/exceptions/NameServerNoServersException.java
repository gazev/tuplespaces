package pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions;

public class NameServerNoServersException extends NameServerException {
  public NameServerNoServersException(String service, String qualifier) {
    super(
        String.format(
            "0 servers for service '%s' and qualifier '%s'", service, qualifier));
  }
}
