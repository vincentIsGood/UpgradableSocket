package com.vincentcodes.net;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class UpgradableSocket implements SSLUpgradableSocket, BasicSocket, Closeable {
    static SSLUpgrader sslUpgrader;

    private Socket socket;
    private boolean useClientMode = true;
    private InputStream is;

    /**
     * @param socket a connected socket
     */
    public UpgradableSocket(Socket socket) throws IOException{
        if(!socket.isConnected())
            throw new IllegalArgumentException("socket is not connected");
        this.socket = socket;
        is = socket.getInputStream();
    }
    public UpgradableSocket(String host, int port) throws IOException{
        this(new Socket(host, port));
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return is;
    }
    @Override
    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    @Override
    public boolean isConnected() {
        return socket.isConnected();
    }

    @Override
    public boolean isClosed() {
        return socket.isClosed();
    }

    @Override
    public InetAddress getLocalAddress() {
        return socket.getLocalAddress();
    }

    @Override
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    /**
     * The addr this socket connected to
     */
    @Override
    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    /**
     * The port this socket connected to
     */
    @Override
    public int getPort() {
        return socket.getPort();
    }

    /**
     * Set the mode of this socket. TLS requires clear 
     * distinction of client and server (eg. ClientHello).
     * @param mode client or server mode
     */
    @Override
    public void setClientMode(boolean mode) {
        useClientMode = mode;
    }

    /**
     * Upgrade the socket.
     * <p>
     * Note: use {@link #setClientMode(boolean)} to set what
     * type of socket you are using. Default client mode.
     * <p>
     * Before anything, create an {@link #sslUpgrader} first.
     * @throws IOException error during the upgrade. Do not 
     * attempt to use the socket after error occured. 
     * @return this socket is sucessfully upgraded or not
     */
    @Override
    public boolean upgrade() throws IOException {
        if(socket instanceof SSLSocket) return true;

        // If it is server or listening side, we use #upgradeSocket(Socket, InputStream, boolean)
        // otherwise, just pure upgrade the connection
        if(useClientMode){
            SSLSocket sslSocket = upgradeSocket(socket, useClientMode);
            sslSocket.startHandshake();
            socket = sslSocket;
            is = socket.getInputStream();
            return true;
        }

        boolean upgraded = false;
        ByteArrayInputStream bais = new ByteArrayInputStream(socket.getInputStream().readNBytes(3));
        bais.mark(3); // allow Checker to peek 3 bytes
        if(TLSHelloSignatureChecker.validateHello(bais)){
            bais.reset(); // reset the peek limit
            SSLSocket sslSocket = upgradeSocket(socket, bais, useClientMode);
            sslSocket.startHandshake();
            socket = sslSocket;
            upgraded = true;
        }else bais.reset();
        is = new SequenceInputStream(bais, socket.getInputStream());
        return upgraded;
    }

    /**
     * Null if this socket is not upgraded
     */
    @Override
    public SSLParameters getSSLParameters() {
        if(socket instanceof SSLSocket)
            return ((SSLSocket)socket).getSSLParameters();
        return null;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
    
    public Socket getUnderlyingSocket(){
        return socket;
    }
    
    // Ref: https://stackoverflow.com/questions/8425999/upgrade-java-socket-to-encrypted-after-issue-starttls
    /**
     * pure upgrade
     */
    private static SSLSocket upgradeSocket(Socket socket, boolean clientMode) throws IOException{
        SSLSocketFactory sslFactory = sslUpgrader.getSSLSocketFactory();
        SSLSocket sslSocket = (SSLSocket)sslFactory.createSocket(socket, socket.getInetAddress().getHostAddress(), socket.getPort(), true);
        sslSocket.setUseClientMode(clientMode);
        return sslSocket;
    }
    
    /**
     * upgrade but some bytes (related to the TLS handshake) are read before
     * @param consumed the bytes read
     */
    private static SSLSocket upgradeSocket(Socket socket, InputStream consumed, boolean clientMode) throws IOException{
        SSLSocketFactory sslFactory = sslUpgrader.getSSLSocketFactory();
        SSLSocket sslSocket = (SSLSocket)sslFactory.createSocket(socket, consumed, true);
        sslSocket.setUseClientMode(clientMode);
        return sslSocket;
    }

    public static void setSSLUpgrader(SSLUpgrader upgrader){
        sslUpgrader = upgrader;
        sslUpgrader.createSSLContext();
    }

    public static SSLUpgrader getSSLUpgrader(){
        return sslUpgrader;
    }
}
