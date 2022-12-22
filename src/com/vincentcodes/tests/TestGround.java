package com.vincentcodes.tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.vincentcodes.net.SSLUpgrader;
import com.vincentcodes.net.UpgradableSocket;

public class TestGround {
    static {
        if(UpgradableSocket.getSSLUpgrader() == null)
            UpgradableSocket.setSSLUpgrader(new SSLUpgrader(new File(".testcerts/keystore.p12"), "changeit"));
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // startServer();
        // startClient();
    }
    public static void startClient() throws IOException, InterruptedException {
        // Create client socket
        try(UpgradableSocket socket = new UpgradableSocket("127.0.0.1", 1234)){
            socket.upgrade();
            socket.getOutputStream().write("AAAAAA".getBytes());
            socket.getOutputStream().write("BAAAAA".getBytes());
            socket.getOutputStream().write("CAAAAA".getBytes());
            socket.getOutputStream().write("DAAAAA".getBytes());
            Thread.sleep(1000);
        }
    }
    public static void startServer() throws IOException {
        try(ServerSocket server = new ServerSocket(1234)){
            Socket conn;
            while((conn = server.accept()) != null){
                try(UpgradableSocket client = new UpgradableSocket(conn)){
                    client.setClientMode(false);
                    client.upgrade();
                    
                    InputStream is = client.getInputStream();
                    byte[] buf = new byte[1024];
                    int len;
                    while((len = is.read(buf)) != -1)
                        System.out.print(new String(buf, 0, len));
                }
            }
        }
    }
}
