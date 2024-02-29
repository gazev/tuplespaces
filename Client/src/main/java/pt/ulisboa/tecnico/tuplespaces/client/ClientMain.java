package pt.ulisboa.tecnico.tuplespaces.client;

import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.*;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.exceptions.NameServerException;

import static java.lang.Math.pow;

public class ClientMain {
  public static boolean DEBUG_MODE = false; // debug flag

  public static void debug(String s) {
    if (DEBUG_MODE) {
      System.err.println("[DEBUG] " + s);
    }
  }

  private static void printUsage() {
    System.err.println(
        "Usage: mvn exec:java -Dexec.args=\"[ns_host] [ns_port] [-h] [-d]\"\n"
            + "\n"
            + "Client for TupleSpace distributed network\n"
            + "\n"
            + "Optional positional arguments:\n"
            + "  ns_host     Name server host IP address (default: localhost)\n"
            + "  ns_port     Name server port            (default: 5001)\n"
            + "Options:\n"
            + "  -h, -help   Show this message and exit\n"
            + "  -d, -debug  Run in debug mode");
  }

  public static void main(String[] args) {
    String nsHost = "localhost"; // default ns_host argument value
    String nsPort = "5001";      // default ns_port argument value

    // check for too much arguments (it will make no sense of positional arguments)
    if (args.length > 3) {
      System.err.println("Too many arguments provided");
      printUsage();
      System.exit(1);
    }

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
        nsHost = args[i];
        if (args.length > i + 1) {
          nsPort = args[i + 1];
          i++;
        }
      }
    }

    // validate port argument
    int nsPortInt;
    try {
      nsPortInt = Integer.parseInt(nsPort);
      if (nsPortInt <= 1024 || nsPortInt >= pow(2, 16)) {
        throw new NumberFormatException(); // will be caught and resume in following catch
      }
    } catch (NumberFormatException e) {
      System.err.println(
              "Invalid 'port' argument, expected an integer in valid port range, got " + nsPort);
      printUsage();
      return;
    }

    // print arguments if in DEBUG_MODE
    for (int i = 0; i < args.length; ++i) {
      debug(String.format("Argument %d: %s", i, args[i]));
    }

    final String nsAddr = nsHost + ":" + nsPort;

    run(nsAddr, "TupleSpaces", "");
  }

  public static void run(String nsAddr, String serviceName, String serviceQualifier) {
    NameServerService nameServer = new NameServerService(nsAddr);
    List<NameServerService.ServiceEntry> serverEntries = null;
    try {
      serverEntries = nameServer.lookup(serviceName, serviceQualifier);
    } catch (NameServerException e) {
      System.err.println("[ERROR] Failed finding servers");
      System.err.println("[ERROR] " + e.getMessage());
      return;
    } finally {
      nameServer.shutdown();
    }

    debug("[INFO] Got " + serverEntries.size() + " servers for service " + serviceName);

    ClientService client;
    try {
      ClientService client = new ClientService(serverEntries);
    } catch (java.lang.IllegalArgumentException e) {
      // TODO for second phase this should be handled inside the constructor and a custom exception
      // should be used for the case where all servers had invalid arguments
      System.err.println("[ERROR] Invalid server entries retreived from name server. Error: " + e .getMessage());
      return;
    }

    CommandProcessor parser = new CommandProcessor(client);
    // start reading input
    parser.parseInput();
    // shutdown client
    client.shutdown();
  }
}
