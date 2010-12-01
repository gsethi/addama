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
import org.systemsbiology.addama.commons.httpclient.support.StatusCodeCaptureResponseCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class ClideServer {
    private static final Logger log = Logger.getLogger(ClideServer.class.getName());

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

    public String[] getClientUris() throws Exception {
        ArrayList<String> uris = new ArrayList<String>();

        JSONObject clients = (JSONObject) httpClientTemplate.executeMethod(new GetMethod(subscribersUri), new OkJsonResponseCallback());
        if (clients != null && clients.has("items")) {
            JSONArray items = clients.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                uris.add(item.getString("uri"));
            }
        }

        log.info(uris.toString());
        return uris.toArray(new String[uris.size()]);
    }

    public void upload(String uri, File... files) throws Exception {
        log.info(uri);

        Part[] parts = new Part[files.length];
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            parts[i] = new FilePart(f.getName(), f);
        }

        GetMethod get = new GetMethod(uri + "/directlink");
        String directLink = (String) httpClientTemplate.executeMethod(get, new DirectLinkResponseCallback());

        PostMethod post = new PostMethod(directLink);
        post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));

        JSONObject json = (JSONObject)httpClientTemplate.executeMethod(post, new OkJsonResponseCallback());
        if (json != null) {
            log.info("response=" + json.toString(4));
        } else {
            log.warning("unknown response");
        }
    }
}
