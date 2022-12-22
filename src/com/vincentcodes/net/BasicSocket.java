package com.vincentcodes.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

public interface BasicSocket {
    InputStream getInputStream() throws IOException;

    OutputStream getOutputStream() throws IOException;

    /**
     * @return whether a connection is successfully made (note: stays true after close)
     */
    boolean isConnected();

    boolean isClosed();

    InetAddress getLocalAddress();
    int getLocalPort();

    InetAddress getInetAddress();
    int getPort();
}
