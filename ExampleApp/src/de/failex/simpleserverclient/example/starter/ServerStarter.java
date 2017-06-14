package de.failex.simpleserverclient.example.starter;

import com.blogspot.debukkitsblog.Net.Datapackage;
import com.blogspot.debukkitsblog.Net.Executable;
import com.blogspot.debukkitsblog.Net.Server;

import java.net.Socket;

/**
 * Erstellt von felix am 13.06.2017 um 17:18 Uhr.
 */
public class ServerStarter extends Server{


    public ServerStarter(int port) {
        super(port, true, true, false);
    }


    @Override
    public void preStart() {
    }
}
