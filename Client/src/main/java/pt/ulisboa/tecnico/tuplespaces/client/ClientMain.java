package pt.ulisboa.tecnico.tuplespaces.client;

import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.*;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerException;

public class ClientMain {
  public static final String qualifier = "A"; // invariant for 1st delivery

  private static String nameServerAddr = "localhost:5001"; // hardcoded address of known name server
  public static boolean DEBUG_MODE = false; // debug flag

  public static void debug(String s) {
    if (DEBUG_MODE) {
      System.err.println("[DEBUG] " + s);
    }
  }

  private static void printUsage() {
    System.out.println(
        "Usage: mvn exec:java -Dexec.args=\"<service> [-h] [-d]\"\n"
            + "\n"
            + "Client for TupleSpace distributed network\n"
            + "\n"
            + "Positional arguments:\n"
            + "  service     Service name (default: TupleSpace)\n"
            + "Options:\n"
            + "  -h, --help  Show this message and exit\n"
            + "  -d, --debug Run in debug mode");
  }

  public static void main(String[] args) {
    // check arguments
    if (args.length < 1) {
      System.err.println("Missing <service> argument");
      printUsage();
      return;
    }

    // check for --help flag
    if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
      printUsage();
      return;
    }

    // check for debug flag;
    if (args.length == 2 && (args[1].equals("-d") || args[1].equals("--debug"))) {
      DEBUG_MODE = true;
      debug("Running in debug mode");
    }

    // print arguments if in DEBUG_MODE
    for (int i = 0; i < args.length; ++i) {
      debug(String.format("Argument %d: %s", i, args[i]));
    }

    final String service = args[0]; // invariant(?) "TupleSpaces"

    NameServerService nameServer = new NameServerService(nameServerAddr);
    nameServer.connect();

    List<NameServerService.ServiceEntry> serverEntries = null;
    try {
      serverEntries = nameServer.lookup(service, ""); // TODO change qualifier for second delivery
    } catch (NameServerException e) {
      System.err.println("[ERROR] Failed finding servers");
      System.err.println("[ERROR] " + e.getMessage());
      return;
    } finally {
      nameServer.shutdown();
    }

    debug(
        "[INFO] Got " + serverEntries.size() + " service entries for " + service + " " + qualifier);

    ClientService client = new ClientService(serverEntries);
    System.out.println("[INFO] Running " + service + " client");

    CommandProcessor parser = new CommandProcessor(client);
    // start reading input
    parser.parseInput();

    // shutdown client
    client.shutdown();
  }
}
