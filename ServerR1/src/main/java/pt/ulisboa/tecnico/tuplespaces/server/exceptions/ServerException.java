package pt.ulisboa.tecnico.tuplespaces.server.exceptions;

public class ServerException extends Exception {
    public ServerException(String subExceptionName, String s) {
        super(String.format("%s: %s", subExceptionName, s));
    }

}
