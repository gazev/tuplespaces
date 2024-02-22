package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {
    public static boolean DEBUG_MODE = false;

    public static void debug(String s) {
        System.err.println("[DEBUG]: " + s);
    }

    public static void main(String[] args) {

        System.out.println(ClientMain.class.getSimpleName());

        // receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }

        // check arguments
        if (args.length < 3) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -Dexec.args=<host> <port> <service_name> [--debug]");
            return;
        }

        // get the host and the port
        final String host = args[0];
        final String port = args[1];

        if (args.length == 4) {
            if (args[3].equals("--debug")) {
                DEBUG_MODE = true;
            }
        }

        String target = host + ":" + port;

        CommandProcessor parser = new CommandProcessor(new ClientService(target, args[2]));
        parser.parseInput();
    }
}
