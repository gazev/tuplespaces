package pt.ulisboa.tecnico.tuplespaces.server;

import static java.lang.Math.pow;

public class ServerMain {

  private static boolean DEBUG_MODE = false;

  private static void debug(String s) {
    if (DEBUG_MODE) {
      System.err.println("[DEBUG] " + s);
    }
  }

  private static void printUsage() {
    System.out.println(
        "Usage: mvn exec:java -Dexec.args=\"<host> <port> <qual> <service> [-h] [-d]\"\n"
            + "\n"
            + "Server for TuplesSpace p2p network server\n"
            + "\n"
            + "Positional arguments:\n"
            + "  host        Name server host address (default: localhost)\n"
            + "  port        Name server port address (default: 2001)\n"
            + "  qual        Server ID                (default: A)\n"
            + "  service     Server service           (default: TupleSpaces)\n"
            + "Options:\n"
            + "  -h, --help  Show this message and exit\n"
            + "  -d, --debug Run in debug mode");
  }

  public static void main(String[] args) {
    if (args.length != 4) {
      System.err.println("Missing positional arguments");
      printUsage();
      return;
    }

    String host = args[0];
    String qual = args[2];
    String service = args[3];
    int port;
    try {
      port = Integer.parseInt(args[1]);
      if (port <= 0 || port >= pow(2, 16)) {
        throw new RuntimeException();
      }
    } catch (RuntimeException e) {
      System.err.println(
          "Invalid 'port' argument, expected and integer in valid port range, got " + args[1]);
      printUsage();
      return;
    }
  }
}
