# UpgradableSocket
A Java implementation of a wrapper socket which allows upgrading itself from a normal socket to an TLS/SSL socket.

## Concept
The method used here is greatly inspired by an answer on [StackOverflow](https://stackoverflow.com/questions/11985896/can-a-java-server-accept-both-ssl-and-plaintext-connections-on-one-port) where we: 
1. use a normal socket to verify whether there is a TLS handshake going on
2. then hide the detection process in the background so that the upgrade happens seamlessly

## Detection Method
There are 2 ways to go about this:
1. Simple Signature Check (**Current method**)
2. Deep Check

### Simple Signature Check
Here, we check for a signature TLS `0x16030y` (where `y = {1,2,3,4}`).

Currently, this is the implementation used to do the detection.

### Deep Check
This method requires checking of the TLS Record Type `ClientHello`. If the whole record is valid, then it is possible that client wants a TLS connection.

## Tests
Tests are located in `src/com/vincentcodes/tests`. One pre-requisite for all test is a keystore, `.testcerts/keystore.p12`. This keystore must be created on your own.

To create one, you can use the `keytool` command. Use the following to create PKCS12 industry keystore format. Quoting [Baeldung](https://www.baeldung.com/spring-boot-https-self-signed-certificate).

```sh
keytool -genkeypair -alias keystore -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 3650
```

Then, move the `keystore.p12` to `.testcerts/` directory.