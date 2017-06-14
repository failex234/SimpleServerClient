package de.failex.simpleserverclient.example;

import com.blogspot.debukkitsblog.Net.Datapackage;
import com.blogspot.debukkitsblog.Net.Executable;
import de.failex.simpleserverclient.example.starter.ClientStarter;
import de.failex.simpleserverclient.example.starter.ServerStarter;

import java.net.Socket;
import java.util.Scanner;

/**
 * Erstellt von felix am 13.06.2017 um 17:18 Uhr.
 */
public class SimpleServerClientExample {

    static ClientStarter client;
//pls
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please start the program with the following arguments:");
            System.out.println("java -jar ExampleApp.jar server 1337 (Replace everything that is different for you)");
            System.out.println("OR");
            System.out.println("java -jar ExampleApp.jar client 88.212.34.12 1337 (Replace everything that is different for you)");
            System.exit(1);
        } else if (args.length < 2) {
            System.out.println("Not enough arguments! Please start the program with the following arguments:");
            System.out.println("<server|client> <ip> <port>");
            System.exit(1);
        } else {
            if (args[0].equalsIgnoreCase("server")) {
                startServer(args[1]);
                startInterpreter("server");
            } else if (args[0].equalsIgnoreCase("client")) {
                startClient(args[1], args[2]);
                startInterpreter("client");
            }
        }

    }

    public static void startInterpreter(String mode) {
        if (mode.equals("server")) {
            System.out.println("Example-Server $ ");
        } else {
            System.out.println("Example-Client $ ");
            Scanner in = new Scanner(System.in);
            String command = in.nextLine().toLowerCase();
            switch (command) {
                case "sendmessage":
                    System.out.print("Enter receivers nickname: ");
                    String name = (new Scanner(System.in)).nextLine();
                    System.out.print("\nEnter message: ");
                    String message = (new Scanner(System.in)).nextLine();
                    System.out.println("\nSending message \"" + message + "\" to " + name + "...");
                    client.sendMessage(new Datapackage("_MSG_", message, name));
                    startInterpreter(mode);
                    break;
                case "broadcast":
                    System.out.print("Enter message to broadcast: ");
                    String message1 = (new Scanner(System.in)).nextLine();
                    System.out.println("\n Broadcasting \"" + message1 + "\"...");
                    client.sendMessage(new Datapackage("_BROADCAST_", message1));
                    break;
                case "setname":
                    System.out.print("Enter name to set: ");
                    String nick = (new Scanner(System.in)).nextLine();
                    client.sendMessage(new Datapackage("_SET_NAME_", nick));
                    break;
                case "getname":
                    client.sendMessage(new Datapackage("_GET_NAME_", "foobar"));
                    break;
                case "help":
                    System.out.println("Available commands:");
                    System.out.println(" sendmessage - Send a message to another client");
                    System.out.println(" broadcast   - Broadcast message to all clients");
                    System.out.println(" setname     - Set own client nickname (required to send messages)");
                    System.out.println(" getname     - Gets current client nickname");
                    System.out.println(" help        - This menu");
                    break;
            }
        }

    }

    public static void startServer(String port) {
        ServerStarter server = new ServerStarter(Integer.parseInt(port));
    }

    public static void startClient(String ip, String port) {
        client = new ClientStarter(ip, Integer.parseInt(port));
        client.registerMethod("_MSG_", (datapackage, socket) -> {
            String message = datapackage.get(1).toString();
            String sender = datapackage.get(2).toString();
            System.out.println(sender + ": " + message);
        });

        client.registerMethod("_BRODACAST_", new Executable() {
            @Override
            public void run(Datapackage datapackage, Socket socket) {
                String message = datapackage.get(1).toString();
                String sender = datapackage.get(2).toString();
                System.out.println("!! BROADCAST !! " + sender + ": " + message);
            }
        });
    }
}
