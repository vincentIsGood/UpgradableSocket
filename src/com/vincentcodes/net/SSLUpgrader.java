package com.vincentcodes.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Background / Pain suffered.
 * HTTPS was tested from another project "working" project, "HttpsServerDemo".
 * To be honest, it somewhat worked, but at that moment I had not idea 
 * what TrustStore and KeyStore is. After more than 12 hours of searching
 * through the internet, only by then, I solved the "certificate_unknown"
 * problem.
 * 
 * Currently, SSLUpgrader only supports jks / pfx / p12 file extensions
 * @see https://www.baeldung.com/java-keystore-truststore-difference
 */
public class SSLUpgrader {
    private File javaKeyStore;
    private String keyStorePass;
    private SSLContext sslContext;

    public SSLUpgrader(File javaKeyStore, String keyStorePass){
        String ext = extractFileExtension(javaKeyStore.getName());
        if(!ext.equals("jks") && !ext.equals("pfx") && !ext.equals("p12"))
            throw new IllegalArgumentException("Unsupported keystore type.");
        this.javaKeyStore = javaKeyStore;
        this.keyStorePass = keyStorePass;
    }

    /**
     * If context is created before, it returns the created context.
     * @return null if the creation process failed
     */
    public SSLContext createSSLContext(){
        if(sslContext != null) return sslContext;
        try{
            // Java KeyStore
            KeyStore keyStore = getKeyStoreInstance();
            keyStore.load(new FileInputStream(javaKeyStore), keyStorePass.toCharArray());

            // KeyStore trustStore = getKeyStoreInstance();
            // trustStore.load(new FileInputStream("./cacerts"), "changeit".toCharArray());

            // A Java keystore stores private key entries, certificates with public keys or just secret keys
            KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmFactory.init(keyStore, keyStorePass.toCharArray());
            KeyManager[] km = kmFactory.getKeyManagers();

            // In Java, we use it to trust the third party (server) we're about to communicate with.
            // We, the client, then look up the associated certificate in our truststore
            TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmFactory.init(keyStore);
            TrustManager[] tm = tmFactory.getTrustManagers();

            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.getServerSessionContext().setSessionCacheSize(100);
            sslContext.init(km, tm, null);
            this.sslContext = sslContext;

            return sslContext;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public SSLContext getSSlContext() {
        return sslContext;
    }

    private KeyStore getKeyStoreInstance() throws KeyStoreException, NullPointerException{
        switch(extractFileExtension(javaKeyStore.getName())){
            case "pfx": return KeyStore.getInstance("PKCS12");
            case "p12": return KeyStore.getInstance("PKCS12");
        }
        return KeyStore.getInstance("JKS");
    }

    public SSLServerSocket getSSLServerSocket(int port) throws IOException{
        SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
        return (SSLServerSocket)serverSocketFactory.createServerSocket(port);
    }

    public SSLSocketFactory getSSLSocketFactory(){
        return sslContext.getSocketFactory();
    }

    public SSLSocket createSSLSocket(String host, int port) throws IOException{
        return (SSLSocket)sslContext.getSocketFactory().createSocket(host, port);
    }

    private static String extractFileExtension(String filename){
        int index = filename.lastIndexOf('.')+1;
        if(index == -1) return null;
        return filename.substring(index);
    }
}
