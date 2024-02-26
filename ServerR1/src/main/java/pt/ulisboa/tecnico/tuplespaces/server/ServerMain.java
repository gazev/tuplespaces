package pt.ulisboa.tecnico.tuplespaces.server;

import pt.ulisboa.tecnico.tuplespaces.server.exceptions.ServerRegisterException;
import pt.ulisboa.tecnico.tuplespaces.server.grpc.NameServerService;

import static java.lang.Math.pow;

public class ServerMain {
  private static final String serviceName = "TupleSpaces";       // service name (invariant)
  private static final String nameServerAddr = "localhost:5001"; // hardcoded address of name server
  private static boolean DEBUG_MODE = false; // debug flag

  /**
   * Show information if running on DEBUG_MODE
   *
   * @param s message to be shown in debug mode
   */
  public static void debug(String s) {
    if (DEBUG_MODE) {
      System.err.println("[DEBUG] " + s);
    }
  }

  /** Print program's usage message */
  private static void printUsage() {
    System.out.println(
        "Usage: mvn exec:java -Dexec.args=\"<port> <qualifier> [-h] [-d]\"\n"
            + "\n"
            + "Server for TuplesSpace distributed network\n"
            + "\n"
            + "Positional arguments:\n"
            + "  port        Port where the server will listen (default: 2001)\n"
            + "  qualifier   Server instance qualifier (default: A)\n"
            + "Options:\n"
            + "  -h, --help  Show this message and exit\n"
            + "  -d, --debug Run in debug mode");
  }

  public static void main(String[] args) {
    // check for valid number of arguments
    if (args.length < 2) {
      System.err.println("Missing positional arguments");
      printUsage();
      return;
    }

    // check for "--help" and "--debug" flags
    if (args.length == 3) {
      if (args[2].equals("--help") || args[2].equals("-h")) {
        printUsage();
        return;
      } else if (args[2].equals("--debug") || args[2].equals("-d")) {
        DEBUG_MODE = true;
      } else {
        System.out.println("[INFO] Got unknown argument %s" + args[2]);
      }
    }

    // print arguments if in DEBUG_MODE
    for (int i = 0; i < args.length; ++i) {
      debug(String.format("Argument %d: %s", i, args[i]));
    }

    // validate port argument
    int port;
    try {
      port = Integer.parseInt(args[0]);
      if (port <= 1024 || port >= pow(2, 16)) {
        throw new NumberFormatException(); // will be caught and resume in following catch
      }
    } catch (NumberFormatException e) {
      System.err.println(
          "Invalid 'port' argument, expected an integer in valid port range, got " + args[0]);
      printUsage();
      return;
    }

    // server qualifier
    String qualifier = args[1];

    // entry point
    run("localhost", port, qualifier, serviceName);
  }

  /**
   * Run a new server instance.
   *
   * @param host server host address (e.g, "localhost")
   * @param port server port (e.g, 2001)
   * @param qual server qualifier (e.g, "A")
   * @param service server service name (e.g, TupleSpaces)
   */
  public static void run(String host, int port, String qual, String service) {
    // class responsible for talking to the name server
    NameServerService nameServerService = new NameServerService(nameServerAddr);

    // inject NameServerService
    Server server = new Server(host, port, qual, service, nameServerService);

    server.setup(); // initialize server

    // register server in name server
    try {
      server.register();
    } catch (ServerRegisterException e) {
      debug(e.getMessage());
      System.err.println("[ERROR] Failed registering server");
      System.err.println("[ERROR] " + e.getMessage());
      System.err.println(
          "[WARN] Clients might be unable to connect if registration on the name server failed");
    }

    server.run(); // blocks running gRPC server
  }
}
