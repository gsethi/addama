/*
**    Copyright (C) 2003-2010 Institute for Systems Biology
**                            Seattle, Washington, USA.
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
*/
package org.systemsbiology.addama.commons.httpclient.support.ssl;

import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The class refererences here:
 * http://svn.apache.org/viewvc/httpcomponents/oac.hc3x/trunk/src/contrib/org/apache/commons/httpclient/contrib/ssl/EasySSLProtocolSocketFactory.java?view=markup
 */
public class EasySSLProtocolSocketFactory implements ProtocolSocketFactory {
    private static final Logger log = Logger.getLogger(EasySSLProtocolSocketFactory.class.getName());

    private SSLContext sslcontext;

    public EasySSLProtocolSocketFactory() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        this.sslcontext = SSLContext.getInstance("SSL");
        this.sslcontext.init(null, new TrustManager[]{new EasyX509TrustManager(null)}, null);
    }

    /*
     * Method Implementations:  ProtocolSocketFactory
     */

    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) throws IOException {
        return this.sslcontext.getSocketFactory().createSocket(host, port, clientHost, clientPort);
    }

    public Socket createSocket(String host, int port, InetAddress address, int localPort, HttpConnectionParams params)
            throws IOException {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }

        int timeout = params.getConnectionTimeout();
        SocketFactory socketfactory = this.sslcontext.getSocketFactory();
        if (timeout == 0) {
            return socketfactory.createSocket(host, port, address, localPort);
        } else {
            Socket socket = socketfactory.createSocket();
            SocketAddress localaddr = new InetSocketAddress(address, localPort);
            SocketAddress remoteaddr = new InetSocketAddress(host, port);
            socket.bind(localaddr);
            socket.connect(remoteaddr, timeout);
            return socket;
        }
    }

    public Socket createSocket(String host, int port) throws IOException {
        return this.sslcontext.getSocketFactory().createSocket(host, port);
    }

    /*
     * Private Classes
     */

    private class EasyX509TrustManager implements X509TrustManager {
        private final X509TrustManager standardTrustManager;

        public EasyX509TrustManager(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(keystore);

            TrustManager[] trustmanagers = factory.getTrustManagers();
            if (trustmanagers.length == 0) {
                throw new NoSuchAlgorithmException("no trust manager found");
            }

            this.standardTrustManager = (X509TrustManager) trustmanagers[0];
        }

        public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
            this.standardTrustManager.checkClientTrusted(certificates, authType);
        }

        public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
            if (certificates != null) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine("Server certificate chain:");
                    for (int i = 0; i < certificates.length; i++) {
                        log.fine("X509Certificate[" + i + "]=" + certificates[i]);
                    }
                }
            }
        }

        public X509Certificate[] getAcceptedIssuers() {
            return this.standardTrustManager.getAcceptedIssuers();
        }
    }
}
