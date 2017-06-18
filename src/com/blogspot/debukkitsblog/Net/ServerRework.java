package com.blogspot.debukkitsblog.Net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by felix on 18.06.17.
 */
public abstract class ServerRework {

    private int port;
    private int timeout;

    private ArrayList<Socket> clients = new ArrayList<>();
    private HashMap<String, Socket> clientnames = new HashMap<>(); //Register on connect / demand nickname on connect
    private HashMap<String, Executable> methods = new HashMap<>();

    private ServerSocket server;

    public ServerRework(int port, int timeout) {
        this.port = port;
        this.timeout = timeout;

        preStart();

        startServer();
    }

    public abstract void preStart();

    public abstract void onClientConnect();

    public abstract void onClientRegister();

    public abstract void onClientUnregister();

    public abstract void onClientDisconnect();


    public void startServer() {
        try {
            server = new ServerSocket(this.port);
        }
        catch(IOException e) {
            log("Error creating Socket!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void log(String msg) {
        System.out.println("[SERVER] " + msg);
    }
}
