package com.vincentcodes.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.vincentcodes.net.SSLUpgrader;
import com.vincentcodes.net.UpgradableSocket;

@TestInstance(Lifecycle.PER_CLASS)
public class RawClientSocketTests {

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
                            OutputStream os = client.getOutputStream();
                            os.write("AAAAAA".getBytes());
                            os.write("BAAAAA".getBytes());
                            os.write("CAAAAA".getBytes());
                            os.write("DAAAAA".getBytes());
                            os.close();

                            // Don't allow server to close the socket too early.
                            // At least give time the client to send stuff.
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
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
    }

    @Test
    public void clientConnecct() throws InterruptedException{
        try(UpgradableSocket socket = new UpgradableSocket("127.0.0.1", 1234)){
            assertTrue(socket.isConnected());
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    @Test
    public void receivedBytes() throws InterruptedException{
        StringBuilder builder = new StringBuilder();
        try(UpgradableSocket socket = new UpgradableSocket("127.0.0.1", 1234)){
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
    public void clientParseWith_NonSSLRecord_error() throws InterruptedException, IOException{
        try(UpgradableSocket socket = new UpgradableSocket("127.0.0.1", 1234)){
            // client connect to a non-ssl server
            // it is client's responsbility to not upgrade the socket
            assertThrows(SSLException.class, ()->{
                socket.upgrade();
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
