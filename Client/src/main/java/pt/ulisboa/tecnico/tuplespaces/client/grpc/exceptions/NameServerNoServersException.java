package pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions;

public class NameServerNoServersException extends NameServerException {
  public NameServerNoServersException(String service, String qualifier) {
    super(
        String.format(
            "No servers available for service '%s' and qualifier '%s'", service, qualifier));
  }
}
