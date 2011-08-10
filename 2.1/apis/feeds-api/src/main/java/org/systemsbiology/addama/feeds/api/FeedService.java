package org.systemsbiology.addama.feeds.api;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientTemplate;
import org.systemsbiology.addama.commons.httpclient.support.OkJsonResponseCallback;

/**
 * @author hrovira
 */
public class FeedService {
    private HttpClientTemplate httpClientTemplate;

    public void setHttpClientTemplate(HttpClientTemplate httpClientTemplate) {
        this.httpClientTemplate = httpClientTemplate;
    }

    /*
     * Public Methods
     */

    public void publishItem(String uri, String msg) throws Exception {
        JSONObject jsonPost = new JSONObject();
        jsonPost.put("text", msg);

        PostMethod post = new PostMethod(uri);
        post.addParameter(new NameValuePair("item", jsonPost.toString()));

        JSONObject resp = (JSONObject) this.httpClientTemplate.executeMethod(post, new OkJsonResponseCallback());
        if (resp == null || !StringUtils.equals(msg, resp.getString("text"))) {
            throw new Exception("error submitting announcement");
        }
    }
}
