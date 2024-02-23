package pt.ulisboa.tecnico.tuplespaces.server;

import pt.ulisboa.tecnico.tuplespaces.server.Server;
import static java.lang.Math.pow;

public class ServerMain {

  private static boolean DEBUG_MODE = false; // debug flag
  private static String nameServerAddr = "localhost:8080"; // hardcoded address of name server

  public static void debug(String s) {
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
    // validate arguments
    if (args.length < 4) {
      if (args.length == 1 && (args[0].equals("--help") || args[0].equals("-h"))) {
        printUsage();
        return;
      }
      System.err.println("Missing positional arguments");
      printUsage();
      return;
    }

    // check for debug flag
    if (args.length == 5 && (args[4].equals("--debug") || args[4].equals("-d"))) {
      DEBUG_MODE = true;
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
          "Invalid 'port' argument, expected an integer in valid port range, got " + args[1]);
      printUsage();
      return;
    }

    run(host, port, qual, service);
  }

  /**
   * Run a new TupleSpaces server.
   *
   * @param host Server host address
   * @param port Server port
   * @param qual Server qualifier
   * @param service Server service name
   */
  public static void run(String host, int port, String qual, String service) {
    Server server = new Server(host, port, qual, service);

    server.registerInNameServer(nameServerAddr);
    server.run();
    server.unregisterInNameServer(nameServerAddr);
  }
}
