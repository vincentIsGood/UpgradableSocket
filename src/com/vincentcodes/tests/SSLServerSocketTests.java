package com.vincentcodes.tests;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.vincentcodes.net.SSLSocketConfigurer;
import com.vincentcodes.net.SSLUpgrader;
import com.vincentcodes.net.UpgradableSocket;

@TestInstance(Lifecycle.PER_CLASS)
public class SSLServerSocketTests {

    static SSLSocketConfigurer sslConfigurer = (params)->{
        System.out.println("[+] Setting configs");
        params.setApplicationProtocols(new String[]{"custom"});
    };

    @BeforeAll
    public void setup(){
        if(UpgradableSocket.getSSLUpgrader() == null)
            UpgradableSocket.setSSLUpgrader(new SSLUpgrader(new File(".testcerts/keystore.p12"), "changeit"));
    }

    @BeforeEach
    public void setupEach(){
        startClient();
    }

    @Test
    public void serverConnecct() throws InterruptedException, IOException{
        try(ServerSocket server = new ServerSocket(1234)){
            // server accepts a client connection and turn it into 
            // a socket (for ssl, it is a server-to-client socket)
            try(UpgradableSocket client = new UpgradableSocket(server.accept())){
                client.setClientMode(false);
                client.upgrade();
                assertTrue(client.isConnected());
            }
        }
    }

    @Test
    public void successfulConfiguration() throws InterruptedException, IOException{
        try(ServerSocket server = new ServerSocket(1234)){
            try(UpgradableSocket client = new UpgradableSocket(server.accept())){
                client.setSSLConfiguerer(sslConfigurer);
                client.setClientMode(false);
                client.upgrade();
                assertTrue(client.isConnected());
                assertEquals("custom", client.getApplicationProtocol());
            }
        }
    }

    @Test
    public void receivedBytes() throws InterruptedException, IOException{
        try(ServerSocket server = new ServerSocket(1234)){
            StringBuilder builder = new StringBuilder();
            try(UpgradableSocket client = new UpgradableSocket(server.accept())){
                client.setClientMode(false);
                client.upgrade();
                InputStream is = client.getInputStream();
                byte[] buf = new byte[1024];
                int len;
                while((len = is.read(buf)) != -1){
                    System.out.println(new String(buf, 0, len));
                    builder.append(new String(buf, 0, len));
                }
            }catch(IOException e){
                e.printStackTrace();
            }
            assertEquals("AAAAAABAAAAACAAAAADAAAAA", builder.toString());
        }
    }

    @Test
    public void serverRecvRawMessage_no_plaintext() throws InterruptedException, IOException{
        try(ServerSocket server = new ServerSocket(1234)){
            try(UpgradableSocket socket = new UpgradableSocket(server.accept())){
                socket.getUnderlyingSocket().setSoTimeout(500);
                assertThrows(SocketTimeoutException.class, ()->{
                    InputStream is = socket.getInputStream();
                    byte[] buf = new byte[1024];
                    int len;
                    // Server is still waiting for response (to establish TLS connection)
                    // Hence, we need timeout
                    while((len = is.read(buf)) != -1)
                        System.out.println(new String(buf, 0, len));
                });
            }
        }
    }

    private static Thread startClient(){
        Thread clientThread = new Thread(){
            @Override
            public void run(){
                // delay the connection to make sure server on the main thread gets to start first.
                // Can use Conditional Variable for this, but delay is just easier to implement.
                try { Thread.sleep(100); } catch (InterruptedException e1) {}
                try(UpgradableSocket socket = new UpgradableSocket("127.0.0.1", 1234)){
                    socket.setSSLConfiguerer(sslConfigurer);
                    socket.upgrade();

                    OutputStream os = socket.getOutputStream();
                    os.write("AAAAAA".getBytes());
                    os.write("BAAAAA".getBytes());
                    os.write("CAAAAA".getBytes());
                    os.write("DAAAAA".getBytes());
                    // os.close(); // do not close it

                    // Don't allow this socket to close too early.
                    // At least give time the client to send stuff.
                    Thread.sleep(200);
                }catch(InterruptedException | IOException e){
                    e.printStackTrace();
                }
            }
        };
        clientThread.start();
        return clientThread;
    }
}
