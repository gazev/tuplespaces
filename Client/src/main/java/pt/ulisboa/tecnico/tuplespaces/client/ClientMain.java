package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.*;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerException;
import pt.ulisboa.tecnico.tuplespaces.nameserver.contract.NameServerOuterClass;

import javax.naming.Name;
import java.util.List;

public class ClientMain {
  // first delivery specification variables
  public static final String qualifier = "A";

  public static boolean DEBUG_MODE = false;
  private static String nameServerAddr = "localhost:5001"; // hardcoded address of known name server

  public static void debug(String s) {
    if (DEBUG_MODE) {
      System.err.println("[DEBUG] " + s);
    }
  }

  private static void printUsage() {
    System.out.println(
        "Usage: mvn:exec -Dexec.args=\"<service> [-h] [-d]\"\n"
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

    if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
      printUsage();
      return;
    }

    if (args.length == 2 && (args[1].equals("-d") || args[1].equals("--debug"))) {
      DEBUG_MODE = true;
      debug("Running in debug mode");
    }

    // print arguments if in DEBUG_MODE
    for (int i = 0; i < args.length; ++i) {
      debug(String.format("Argument %d: %s", i, args[i]));
    }


    final String service = args[0];

    NameServerService nameServer = new NameServerService(nameServerAddr);
    nameServer.connect();
    List<NameServerService.ServiceEntry> serverEntries = null;
    try {
      serverEntries = nameServer.lookup(service, ""); // TODO change for second
    } catch (NameServerException e) {
      debug(e.getMessage());
      System.err.println("[ERROR] Couldn't retrieve servers from name server.");
      return;
    } finally {
      nameServer.shutdown();
    }

    debug("[INFO] Got " + serverEntries.size() + " service entries for " + service + " " + qualifier);

    ClientService client = new ClientService(service, serverEntries);
    CommandProcessor parser = new CommandProcessor(client);
    // start reading input
    parser.parseInput();

    // shutdown client
    client.shutdown();
  }
}
