package fr.univparis1.tomcat;

import java.io.IOException;

import org.apache.coyote.http11.Http11NioProtocol;


public class EarlyCloseHttp11NioProtocol extends Http11NioProtocol {

    public EarlyCloseHttp11NioProtocol() {
        // we need a subclass of NioEndpoint to access doCloseServerSocket()
        super(new EarlyCloseNioEndpoint());
    }

    public void closeServerSocketGraceful() {
        // We want to close socket even if bindState == BOUND_ON_INIT
        // But we can't trick existing AbstractEndpoint.closeServerSocketGraceful .
        // So we duplicate it here ( it is mostly doing AbstractEndpoint.pause() + closing the server socket)

        // But we also can't override AbstractEndpoint.closeServerSocketGraceful
        // So we override the caller ( AbstractProtocol.closeServerSocketGraceful )
        // which is just calling endpoint.closeServerSocketGraceful()

        getEndpoint().pause();
        try {
            ((EarlyCloseNioEndpoint) getEndpoint()).doCloseServerSocket_();
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
