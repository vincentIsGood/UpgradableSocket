package com.vincentcodes.net;

import java.io.IOException;

import javax.net.ssl.SSLParameters;

public interface SSLUpgradableSocket {
    void setClientMode(boolean mode);

    /**
     * Upgrade the socket.
     * <p>
     * Before anything, create an {@link #sslUpgrader} first.
     * @throws IOException error during the upgrade. Do not 
     * attempt to use the socket after error occured. 
     * @return this socket is sucessfully upgraded or not
     */
    boolean upgrade() throws IOException;
    
    SSLParameters getSSLParameters();

    String getApplicationProtocol();
}
