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
import org.systemsbiology.addama.commons.httpclient.support.ssl.EasySSLProtocolSocketFactory;
import org.systemsbiology.addama.commons.spring.PropertiesFileLoader;

import java.net.URL;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author hrovira
 */
public class GaeHostConfiguration extends HostConfiguration implements InitializingBean {
    private static final Logger log = Logger.getLogger(GaeHostConfiguration.class.getName());
    private static final String KEY = "httpclient.secureHostUrl";

    private PropertiesFileLoader propertiesFileLoader;
    private ProtocolSocketFactory protocolSocketFactory;

    public void setPropertiesFileLoader(PropertiesFileLoader propertiesFileLoader) {
        this.propertiesFileLoader = propertiesFileLoader;
    }

    public void setProtocolSocketFactory(ProtocolSocketFactory protocolSocketFactory) {
        this.protocolSocketFactory = protocolSocketFactory;
    }

    public void afterPropertiesSet() throws Exception {
        if (protocolSocketFactory == null) {
            protocolSocketFactory = new EasySSLProtocolSocketFactory();
        }

        if (propertiesFileLoader.loaded() && propertiesFileLoader.has(KEY)) {
            String hostUrl = propertiesFileLoader.getProperty(KEY);
            if (!isEmpty(hostUrl)) {
                URL secureHostUrl = new URL(hostUrl);
                Protocol.registerProtocol("https", new Protocol("https", protocolSocketFactory, 443));
                super.setHost(secureHostUrl.getHost(), secureHostUrl.getPort(), secureHostUrl.getProtocol());
                return;
            }
        }

        log.warning("addama registry host URL not configured in 'addama.properties' [" + KEY + "]");
    }

}