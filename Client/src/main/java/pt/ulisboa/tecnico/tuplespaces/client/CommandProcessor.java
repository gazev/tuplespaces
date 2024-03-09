package pt.ulisboa.tecnico.tuplespaces.client;

import static pt.ulisboa.tecnico.tuplespaces.client.Client.rpcRetry;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;

import java.util.Scanner;

public class CommandProcessor {

  private static final String SPACE = " ";
  private static final String BGN_TUPLE = "<";
  private static final String END_TUPLE = ">";
  public static final String PUT = "put";
  public static final String READ = "read";
  public static final String TAKE = "take";
  private static final String SLEEP = "sleep";
  private static final String SET_DELAY = "setdelay";
  private static final String CLEAR = "clear";
  private static final String EXIT = "exit";
  public static final String GET_TUPLE_SPACES_STATE = "getTupleSpacesState";

  private final Client client;
  private final OrderedDelayer orderedDelayer;

  public CommandProcessor(Client client) {
    this.client = client;
    this.orderedDelayer = new OrderedDelayer(3);
  }

  void parseInput() {

    Scanner scanner = new Scanner(System.in);
    boolean exit = false;

    while (!exit) {
      System.out.print("> ");
      String line = scanner.nextLine().trim();
      String[] split = line.split(SPACE);
      switch (split[0]) {
        case PUT:
          this.put(split);
          break;

        case READ:
          this.read(split);
          break;

        case TAKE:
          this.take(split);
          break;

        case GET_TUPLE_SPACES_STATE:
          this.getTupleSpacesState(split);
          break;

        case SLEEP:
          this.sleep(split);
          break;

        case SET_DELAY:
          this.setdelay(split);
          break;

        case CLEAR:
          System.out.println("\033[2J\033[H");
          break;

        case EXIT:
          exit = true;
          break;

        default:
          this.printUsage();
          break;
      }
    }
    scanner.close();
  }

  private void put(String[] split) {
    // check if input is valid
    if (!this.inputIsValid(split)) {
      this.printUsage();
      return;
    }

    // get the tuple
    String tuple = split[1];

    client.executeTupleSpacesCommand(PUT, tuple, rpcRetry);
  }

  private void read(String[] split) {
    // check if input is valid
    if (!this.inputIsValid(split)) {
      this.printUsage();
      return;
    }

    // get the tuple
    String tuple = split[1];

    client.executeTupleSpacesCommand(READ, tuple, rpcRetry);
  }

  private void take(String[] split) {
    // check if input is valid
    if (!this.inputIsValid(split)) {
      this.printUsage();
      return;
    }

    String tuple = split[1];

    client.executeTupleSpacesCommand(TAKE, tuple, rpcRetry);
  }

  private void getTupleSpacesState(String[] split) {
    if (split.length != 2) {
      this.printUsage();
      return;
    }

    String qualifier = split[1];

    client.executeTupleSpacesCommand(GET_TUPLE_SPACES_STATE, qualifier, rpcRetry);
  }

  private void sleep(String[] split) {
    if (split.length != 2) {
      this.printUsage();
      return;
    }

    // checks if input String can be parsed as an Integer
    long time;
    try {
      time = Integer.parseInt(split[1]);
    } catch (NumberFormatException e) {
      this.printUsage();
      return;
    }

    try {
      Thread.sleep(time * 1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void setdelay(String[] split) {
    if (split.length != 3) {
      this.printUsage();
      return;
    }
    String qualifier = split[1];
    int time;

    // checks if input String can be parsed as an Integer
    try {
      time = Integer.parseInt(split[2]);
    } catch (NumberFormatException e) {
      this.printUsage();
      return;
    }

    int delay = orderedDelayer.setDelay(qualifier.hashCode(), time);

    if (delay != -1) {
        System.out.println("Delay set for server " + qualifier + ": " + delay + " seconds");
    } else {
        System.out.println("Server " + qualifier + " not found");
    }
  }

  private void printUsage() {
    System.out.println(
        "Usage:\n"
            + "- put <element[,more_elements]>\n"
            + "- read <element[,more_elements]>\n"
            + "- take <element[,more_elements]>\n"
            + "- getTupleSpacesState <server>\n"
            + "- sleep <integer>\n"
            + "- setdelay <server> <integer>\n"
            + "- clear\n"
            + "- exit\n");
  }

  private boolean inputIsValid(String[] input) {
    if (input.length < 2
        || !input[1].startsWith(BGN_TUPLE)
        || !input[1].endsWith(END_TUPLE)
        || input.length > 2) {
      this.printUsage();
      return false;
    }

    return true;
  }
}
