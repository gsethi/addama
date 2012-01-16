package org.systemsbiology.addama.registry;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientResponseException;
import org.systemsbiology.addama.commons.httpclient.support.ResponseCallback;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class RegistrationCallback implements ResponseCallback {
    private static final Logger log = Logger.getLogger(RegistrationCallback.class.getName());

    private final ServiceConfig serviceConfig;

    public RegistrationCallback(ServiceConfig service) {
        this.serviceConfig = service;
    }

    public Object onResponse(int statusCode, HttpMethod method) throws HttpClientResponseException {
        String message = "";
        String registryKey = null;
        try {
            if (statusCode == 200) {
                Header header = method.getResponseHeader("x-addama-registry-key");
                if (header != null) {
                    registryKey = header.getValue();
                }
            }
            message = method.getResponseBodyAsString();
        } catch (Exception e) {
            log.warning(e.getMessage());
        } finally {
            StringBuilder builder = new StringBuilder();
            builder.append("\n===============================================\n");
            builder.append("Service Registration");
            builder.append("\n\t").append(serviceConfig.LABEL());
            builder.append(" [ ").append(statusCode).append(" ]:[").append(message).append("]");

            for (Mapping mapping : serviceConfig.getMappings()) {
                builder.append("\n\t").append(mapping.LABEL());
            }
            builder.append("\n===============================================\n");
            log.info(builder.toString());
        }

        return registryKey;
    }

}
