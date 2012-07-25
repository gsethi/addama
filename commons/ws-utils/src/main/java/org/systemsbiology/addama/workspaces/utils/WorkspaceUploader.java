package org.systemsbiology.addama.workspaces.utils;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.protocol.Protocol;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.httpclient.support.*;
import org.systemsbiology.addama.commons.httpclient.support.ssl.EasySSLProtocolSocketFactory;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

import static org.apache.commons.httpclient.protocol.Protocol.registerProtocol;
import static org.apache.commons.lang.StringUtils.*;

/**
 * @author hrovira
 */
public class WorkspaceUploader {
    private static final Logger log = Logger.getLogger(WorkspaceUploader.class.getName());

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("arguments [apikey-file target-uri local-dir]");
        }

        String apikeyFile = args[0];

        String uploadUri = chomp(replace(args[1], " ", "%20"));

        File localFile = new File(args[2]);
        if (!localFile.exists()) {
            throw new IllegalArgumentException("[" + args[2] + "] not found");
        }

        log.info("\n\tpushing:" + localFile.getPath() + "\n\tto:" + uploadUri + "\n\n");

        HttpClientTemplate httpClientTemplate = newHttpClientTemplate(apikeyFile);

        if (localFile.isDirectory()) {
            File[] files = localFile.listFiles();

            Part[] parts = new Part[files.length];
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                parts[i] = new FilePart(f.getName(), f);
            }

            upload(httpClientTemplate, uploadUri + "/" + localFile.getName(), parts);
        } else {
            upload(httpClientTemplate, uploadUri, new FilePart(localFile.getName(), localFile));
        }
    }

    private static void upload(HttpClientTemplate httpClientTemplate, String uploadUri, Part... parts) throws Exception {
        GetMethod get = new GetMethod(uploadUri + "/directlink");
        String directLink = (String) httpClientTemplate.executeMethod(get, new DirectLinkResponseCallback());

        PostMethod post = new PostMethod(directLink);
        post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));

        JSONObject response = (JSONObject) httpClientTemplate.executeMethod(post, new OkJsonResponseCallback());
        if (response != null) {
            log.info("\nupload successful\n");
        } else {
            log.warning("\ninvalid response\n");
        }
    }

    private static HttpClientTemplate newHttpClientTemplate(String apikeyfile) throws Exception {
        Properties p = new Properties();
        p.load(new FileInputStream(new File(apikeyfile)));
        if (p.isEmpty()) {
            throw new IllegalArgumentException("apikey file is missing properties [host,apikey]");
        }

        String host = p.getProperty("host");
        if (isEmpty(host)) {
            throw new IllegalArgumentException("apikey file is missing [host] property");
        }

        final String apikey = p.getProperty("apikey");
        if (isEmpty(apikey)) {
            throw new IllegalArgumentException("apikey file is missing [apikey] property");
        }

        HttpClient httpClient = new HttpClient();
        httpClient.setHttpConnectionManager(new SimpleHttpConnectionManager());
        httpClient.setHostConfiguration(getHostConfiguration(p));

        HttpClientTemplate template = new HttpClientTemplate(httpClient) {
            public Object executeMethod(HttpMethod method, ResponseCallback responseCallback)
                    throws HttpClientException, HttpClientResponseException {
                method.setRequestHeader("x-addama-apikey", apikey);
                return super.executeMethod(method, responseCallback);
            }
        };
        template.setConnectionTimeout(5000);
        template.afterPropertiesSet();
        return template;
    }

    private static HostConfiguration getHostConfiguration(Properties p) throws Exception {
        URL secureHostUrl = new URL("https://" + p.getProperty("host"));

        HostConfiguration hc = new HostConfiguration();
        registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
        hc.setHost(secureHostUrl.getHost(), secureHostUrl.getPort(), secureHostUrl.getProtocol());
        return hc;
    }
}
