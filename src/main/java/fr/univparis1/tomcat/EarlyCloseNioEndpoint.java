package fr.univparis1.tomcat;

import java.io.IOException;
import org.apache.tomcat.util.net.NioEndpoint;


// simple wrapper to give access to NioEndpoint.doCloseServerSocket
public class EarlyCloseNioEndpoint extends NioEndpoint {
    public void doCloseServerSocket_() throws IOException {
        doCloseServerSocket();
    }
}