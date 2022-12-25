package com.vincentcodes.tests;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.vincentcodes.net.SSLSocketConfigurer;
import com.vincentcodes.net.SSLUpgrader;
import com.vincentcodes.net.UpgradableSocket;

@TestInstance(Lifecycle.PER_CLASS)
public class SSLClientSocketTests {

    static SSLSocketConfigurer sslConfigurer = (params)->{
        System.out.println("[+] Setting configs");
        params.setApplicationProtocols(new String[]{"custom"});
    };

    private Thread listenThread;
    private ServerSocket server;

    @BeforeAll
    public void setup(){
        if(UpgradableSocket.getSSLUpgrader() == null)
            UpgradableSocket.setSSLUpgrader(new SSLUpgrader(new File(".testcerts/keystore.p12"), "changeit"));
    
        listenThread = new Thread(){
            @Override
            public void run(){
                try{
                    server = new ServerSocket(1234);
                    Socket conn;
                    while((conn = server.accept()) != null){
                        try(UpgradableSocket client = new UpgradableSocket(conn)){
                            client.setSSLConfiguerer(sslConfigurer);
                            client.setClientMode(false);
                            client.upgrade();

                            OutputStream os = client.getOutputStream();
                            os.write("AAAAAA".getBytes());
                            os.write("BAAAAA".getBytes());
                            os.write("CAAAAA".getBytes());
                            os.write("DAAAAA".getBytes());
                            // os.close(); // do not close it

                            // Don't allow server to close the socket too early.
                            // At least give time the client to send stuff.
                            Thread.sleep(200);
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }catch(IOException e){
                    e.printStackTrace();
                }finally{
                    try {
                        server.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return;
            }
        };
        listenThread.start();

        // Make sure server is started before anything else
        try { Thread.sleep(100); } catch (InterruptedException e1) {}
    }

    @Test
    public void clientConnecct() throws InterruptedException{
        try(UpgradableSocket socket = new UpgradableSocket("127.0.0.1", 1234)){
            socket.upgrade();
            assertTrue(socket.isConnected());
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    @Test
    public void successfulConfiguration() throws InterruptedException{
        try(UpgradableSocket socket = new UpgradableSocket("127.0.0.1", 1234)){
            socket.setSSLConfiguerer(sslConfigurer);
            socket.upgrade();
            assertTrue(socket.isConnected());
            assertEquals("custom", socket.getApplicationProtocol());
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    @Test
    public void receivedBytes() throws InterruptedException{
        StringBuilder builder = new StringBuilder();
        try(UpgradableSocket socket = new UpgradableSocket("127.0.0.1", 1234)){
            socket.upgrade();
            InputStream is = socket.getInputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len = is.read(buf)) != -1)
                builder.append(new String(buf, 0, len));
        }catch(IOException e){
            e.printStackTrace();
        }
        assertEquals("AAAAAABAAAAACAAAAADAAAAA", builder.toString());
    }

    @Test
    public void clientRecvRawMessage_no_plaintext() throws InterruptedException, IOException{
        try(UpgradableSocket socket = new UpgradableSocket("127.0.0.1", 1234)){
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

    @AfterAll
    public void close(){
        listenThread.interrupt();
        try {
            server.close();
            listenThread.join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
