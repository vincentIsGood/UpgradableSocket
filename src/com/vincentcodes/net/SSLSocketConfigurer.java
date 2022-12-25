package com.vincentcodes.net;

import javax.net.ssl.SSLParameters;

@FunctionalInterface
public interface SSLSocketConfigurer {
    void configure(SSLParameters params);
}
