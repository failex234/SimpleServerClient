package de.failex.simpleserverclient.example.starter;

import com.blogspot.debukkitsblog.Net.Datapackage;
import com.blogspot.debukkitsblog.Net.Executable;
import com.blogspot.debukkitsblog.Net.Server;
import com.blogspot.debukkitsblog.Net.ServerRework;

import java.net.Socket;

/**
 * Erstellt von felix am 13.06.2017 um 17:18 Uhr.
 */
public class ServerStarter extends ServerRework{


    public ServerStarter(int port) {
        super(port, 0);
    }


    @Override
    public void preStart() {
    }
}
