package pt.ulisboa.tecnico.tuplespaces.server;

import pt.ulisboa.tecnico.tuplespaces.server.exceptions.ServerRegisterException;
import pt.ulisboa.tecnico.tuplespaces.server.grpc.NameServerService;

import static java.lang.Math.pow;

public class ServerMain {
  private static final String serviceName = "TupleSpaces"; // service name (invariant)
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
    System.err.println(
        "Usage: mvn exec:java -Dexec.args=\"<port> <qualifier> [host] [ns_host] [ns_port] [-h] [-d]\"\n"
            + "\n"
            + "Server for TuplesSpace distributed network\n"
            + "\n"
            + "Positional arguments:\n"
            + "  port        Server port\n"
            + "  qualifier   Server instance qualifier\n"
            + "Optional positional arguments:\n"
            + "  host        Server host IP address      (default: localhost)\n"
            + "  ns_host     Name server host IP address (default: localhost)\n"
            + "  ns_port     Name server port            (default: 5001)\n"
            + "Options:\n"
            + "  -h, -help  Show this message and exit\n"
            + "  -d, -debug Run in debug mode");
  }

  public static void main(String[] args) {
    String host = "localhost";
    String nsHost = "localhost";
    String nsPort = "5001";

    // check for valid number of arguments
    if (args.length < 2) {
      if (args.length == 1 && (args[0].equals("-h") || args[0].equals("-help"))) {
        printUsage();
        System.exit(0);
      }
      System.err.println("Invalid number of arguments");
      printUsage();
      System.exit(1);
    }

    String qualifier = null;
    String port = null;

    // parse arguments
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("-")) { // validate options
        switch (args[i]) {
          case "-h":
          case "-help":
          case "--help":
            printUsage();
            System.exit(0);
            break;
          case "-d":
          case "-debug":
          case "--debug":
            DEBUG_MODE = true;
            break;
          default:
            System.err.println("Unknown option: " + args[i]);
            printUsage();
            System.exit(1);
        }
      } else { // validate positional arguments
        if (i == 0) {
          port = args[i];
        } else if (i == 1) {
          qualifier = args[i];
        } else if (i == 2) {
          host = args[i];
        } else if (i == 3) {
          nsHost = args[i];
          if (args.length > i + 1) {
            nsPort = args[i + 1];
            i++;
          }
        }
      }
    }

    // check if positional arguments were set
    if (qualifier == null || port == null) {
      System.err.println("Missing positional arguments");
      printUsage();
      System.exit(1);
    }

    // print arguments if in DEBUG_MODE
    for (int i = 0; i < args.length; ++i) {
      debug(String.format("Argument %d: %s", i, args[i]));
    }

    // validate port argument
    int portInt;
    try {
      portInt = Integer.parseInt(port);
      if (portInt <= 1024 || portInt >= pow(2, 16)) {
        throw new NumberFormatException(); // will be caught and resume in following catch
      }
    } catch (NumberFormatException e) {
      System.err.println(
          "Invalid 'port' argument, expected an integer in valid port range, got " + args[0]);
      printUsage();
      return;
    }

    // validate ns port argument
    int nsPortInt;
    try {
      nsPortInt = Integer.parseInt(nsPort);
      if (nsPortInt <= 1024 || nsPortInt >= pow(2, 16)) {
        throw new NumberFormatException(); // will be caught and resume in following catch
      }
    } catch (NumberFormatException e) {
      System.err.println(
              "Invalid 'ns_port' argument, expected an integer in valid port range, got " + args[0]);
      printUsage();
      return;
    }

    String serverAddr = host + ":" + port;
    String nsAddr = nsHost + ":" + nsPort;

    // entry point
    run("TupleSpaces", serverAddr, qualifier, nsAddr);
  }

  /**
   * Run a new server instance.
   *
   * @param serverAddr String of server instance address (e.g localhost:2001)
   * @param qualifier  Server instance qualifier         (e.g "A")
   * @param nsAddr     String of name server address     (e.g "localhost:5001)
   */
  public static void run(String serviceName, String serverAddr, String qualifier, String nsAddr) {
    // class responsible for talking to the name server
    NameServerService nameServerService = new NameServerService(nsAddr);
    // injects NameServerService in Server object
    Server server = new Server(serviceName, serverAddr, qualifier, nameServerService);
    try {
      // register server in name server
      server.register();
    } catch (ServerRegisterException e) {
      System.err.println("[ERROR] Failed registering server");
      System.err.println("[ERROR] " + e.getMessage());
      server.getNameServerService().shutdown();
      return;
    }

    server.run(); // blocks running gRPC server
  }
}
