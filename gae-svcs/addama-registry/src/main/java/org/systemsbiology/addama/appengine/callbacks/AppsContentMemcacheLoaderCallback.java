package org.systemsbiology.addama.appengine.callbacks;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.urlfetch.HTTPResponse;
import org.systemsbiology.addama.appengine.memcache.MemcacheLoaderCallback;
import org.systemsbiology.addama.appengine.pojos.HTTPResponseContent;

import java.io.Serializable;
import java.net.URL;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.urlfetch.URLFetchServiceFactory.getURLFetchService;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang.StringUtils.chomp;

/**
 * @author hrovira
 */
public class AppsContentMemcacheLoaderCallback implements MemcacheLoaderCallback {
    private static final Logger log = Logger.getLogger(AppsContentMemcacheLoaderCallback.class.getName());

    private final String appsId;
    private final boolean serveHomepage;

    public AppsContentMemcacheLoaderCallback(String appsId) {
        this(appsId, false);
    }

    public AppsContentMemcacheLoaderCallback(String appsId, boolean serveHomepage) {
        this.appsId = appsId;
        this.serveHomepage = serveHomepage;
    }

    public Serializable getCacheableObject(String contentUri) throws Exception {
        Entity e = getDatastoreService().get(createKey("apps-content", appsId));

        if (serveHomepage) {
            if (e.hasProperty("homepage")) {
                contentUri = "/" + e.getProperty("homepage").toString();
            } else {
                contentUri = "/index.html";
            }
        }

        String url = chomp(e.getProperty("url").toString(), "/");
        log.info("loading:" + url + contentUri);
        URL contentUrl = new URL(url + contentUri);
        HTTPResponse resp = getURLFetchService().fetch(contentUrl);
        if (resp.getResponseCode() == SC_OK) {
            log.info("loaded:" + contentUrl);
            return new HTTPResponseContent(resp);
        }
        return null;
    }
}
