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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * @author hrovira
 */
public class GoogleStorageApiHttpClientTemplate extends HttpClientTemplate {
    private String userIdentity;
    private String secretKey;
    private String host;

    public GoogleStorageApiHttpClientTemplate() {
    }

    public GoogleStorageApiHttpClientTemplate(HttpClient httpClient) {
        super(httpClient);
    }

    /*
     * Dependency Injection
     */

    public void setUserIdentity(String userIdentity) {
        this.userIdentity = userIdentity;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setHost(URL url) {
        this.host = url.getHost();
    }

    /*
     * HttpClientTemplate
     */

    public Object executeMethod(HttpMethod httpMethod, ResponseCallback responseCallback)
            throws HttpClientException, HttpClientResponseException {
        String datetime = getCurrentDateGMT();

        String hmac = getSignature(httpMethod, datetime);

        httpMethod.addRequestHeader("Host", host);
        httpMethod.addRequestHeader("Date", datetime);
        httpMethod.addRequestHeader("Authorization", "GOOG1 " + userIdentity + ":" + hmac);

        return super.executeMethod(httpMethod, responseCallback);
    }

    /*
     * Private Methods
     */

    private String getSignature(HttpMethod httpMethod, String datetime) throws HttpClientException {
        try {
            StringBuilder builder = getData(httpMethod, datetime);

            String data = builder.toString();
            SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(), "HmacSHA1");

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(data.getBytes());

            return new String(Base64.encodeBase64(rawHmac));
        } catch (Exception e) {
            throw new HttpClientException(httpMethod, e);
        }
    }

    private StringBuilder getData(HttpMethod httpMethod, String datetime) {
        StringBuilder builder = new StringBuilder();
        if (httpMethod instanceof GetMethod) {
            builder.append("GET");
            builder.append("\n");
            builder.append("\n");
            builder.append("\n");
            builder.append(datetime);
            builder.append("\n");
            builder.append(httpMethod.getPath());
        }
        return builder;
    }

    private String getCurrentDateGMT() {
        Calendar gmtcal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, gmtcal.get(Calendar.HOUR_OF_DAY));

        SimpleDateFormat sdf = new SimpleDateFormat("E',' dd MMM yyyy HH:mm:ss 'GMT'");
        return sdf.format(cal.getTime());
    }
}
