package com.vincentcodes.net;

import java.io.IOException;
import java.io.InputStream;

/**
 * @see https://www.rfc-editor.org/rfc/rfc8446#section-5.1
 */
public class TLSHelloSignatureChecker {
    /**
     * @param is 3 bytes will be consumed in the process.
     */
    public static boolean validateHello(InputStream is) throws IOException{
        byte[] threeBytesBuf = new byte[3];
        is.read(threeBytesBuf);
        if(threeBytesBuf[0] != 0x16) return false;
        if(threeBytesBuf[1] != 0x03) return false;

        // TLS 1.0 == 0x0301, TLS1.1 == 0x0302, 
        // TLS1.2 == 0x0303, TLS1.3 still use 0x0303
        // acceptable TLS1.3 == 0x0304
        if(threeBytesBuf[2] == 0 || threeBytesBuf[2] > 4) return false;
        return true;
    }
}