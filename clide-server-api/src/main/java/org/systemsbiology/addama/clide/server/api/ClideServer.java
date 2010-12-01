package org.systemsbiology.addama.clide.server.api;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.json.JSONArray;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.httpclient.support.DirectLinkResponseCallback;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientTemplate;
import org.systemsbiology.addama.commons.httpclient.support.OkJsonResponseCallback;

import java.io.File;

/**
 * @author hrovira
 */
public class ClideServer {
    private HttpClientTemplate httpClientTemplate;
    private String subscribersUri;

    /*
     * Dependency Injection
     */

    public void setHttpClientTemplate(HttpClientTemplate httpClientTemplate) {
        this.httpClientTemplate = httpClientTemplate;
    }

    public void setSubscribersUri(String subscribersUri) {
        this.subscribersUri = subscribersUri;
    }

    /*
     * API Methods
     */

    public JSONArray getClients() throws Exception {
        JSONObject clients = (JSONObject) httpClientTemplate.executeMethod(new GetMethod(subscribersUri), new OkJsonResponseCallback());
        if (clients != null && clients.has("items")) {
            return clients.getJSONArray("items");
        }

        return null;
    }

    public JSONObject push(String uri, File... files) throws Exception {
        Part[] parts = new Part[files.length];
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            parts[i] = new FilePart(f.getName(), f);
        }

        GetMethod get = new GetMethod(uri + "/directlink");
        String directLink = (String) httpClientTemplate.executeMethod(get, new DirectLinkResponseCallback());

        PostMethod post = new PostMethod(directLink);
        post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));

        return (JSONObject) httpClientTemplate.executeMethod(post, new OkJsonResponseCallback());
    }
}
