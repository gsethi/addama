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
package org.systemsbiology.addama.commons.httpclient.support;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.springframework.beans.factory.InitializingBean;

import java.net.URL;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class AddamaHostConfiguration extends HostConfiguration implements InitializingBean {
    private static final Logger log = Logger.getLogger(AddamaHostConfiguration.class.getName());

    private URL hostUrl;
    private Integer securePort;
    private ProtocolSocketFactory protocolSocketFactory;

    public void setHostUrl(URL hostUrl) {
        this.hostUrl = hostUrl;
    }

    public void setSecurePort(Integer securePort) {
        this.securePort = securePort;
    }

    public void setProtocolSocketFactory(ProtocolSocketFactory protocolSocketFactory) {
        this.protocolSocketFactory = protocolSocketFactory;
    }

    public void afterPropertiesSet() throws Exception {
        if (securePort != null && protocolSocketFactory != null) {
            Protocol.registerProtocol("https", new Protocol("https", protocolSocketFactory, securePort));
        } else {
            log.warning("secure port not set");
        }

        super.setHost(hostUrl.getHost(), hostUrl.getPort(), hostUrl.getProtocol());
    }
}
