package pt.ulisboa.tecnico.tuplespaces.server.exceptions;

public class InvalidSearchPatternException extends Exception {
    public InvalidSearchPatternException(String s) {
        super("Invalid search pattern " + s);
    }
}
