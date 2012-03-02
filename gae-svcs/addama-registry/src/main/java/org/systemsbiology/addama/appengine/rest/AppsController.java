package org.systemsbiology.addama.appengine.rest;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.memcache.MemcacheService;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.appengine.callbacks.AppsContentMemcacheLoaderCallback;
import org.systemsbiology.addama.appengine.datastore.PutEntityTransactionCallback;
import org.systemsbiology.addama.commons.web.editors.JSONObjectPropertyEditor;
import org.systemsbiology.addama.appengine.pojos.HTTPResponseContent;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.systemsbiology.addama.appengine.datastore.DatastoreServiceTemplate.inTransaction;
import static org.systemsbiology.addama.appengine.memcache.MemcacheServiceTemplate.loadIfNotExisting;
import static org.systemsbiology.addama.appengine.pojos.HTTPResponseContent.serveContent;
import static org.systemsbiology.addama.appengine.util.Users.checkAdmin;

/**
 * @author hrovira
 */
@Controller
public class AppsController {
    private static final Logger log = Logger.getLogger(AppsController.class.getName());
    private final DatastoreService datastore = getDatastoreService();

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(JSONObject.class, new JSONObjectPropertyEditor());
    }

    /*
    * Controllers
    */
    @RequestMapping(value = "/apps", method = RequestMethod.GET)
    protected ModelAndView listAll() throws Exception {
        JSONObject json = new JSONObject();

        PreparedQuery pq = datastore.prepare(new Query("apps-content"));
        for (Entity e : pq.asIterable()) {
            JSONObject item = new JSONObject();
            String id = e.getKey().getName();
            item.put("id", id);
            item.put("uri", "/addama/apps/" + id);
            item.put("label", e.getProperty("label").toString());
            item.put("url", e.getProperty("url").toString());
            if (e.hasProperty("homepage")) {
                item.put("homepage", e.getProperty("homepage").toString());
            } else {
                item.put("homepage", "index.html");
            }
            if (e.hasProperty("logo")) item.put("logo", e.getProperty("logo").toString());
            if (e.hasProperty("description")) item.put("description", e.getProperty("description").toString());
            json.append("items", item);
        }
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/apps/{appsId}", method = RequestMethod.GET)
    protected ModelAndView fetchApp(HttpServletRequest request, HttpServletResponse response,
                                    @PathVariable("appsId") String appsId) throws Exception {
        log.info(appsId);
        AppsContentMemcacheLoaderCallback callback = new AppsContentMemcacheLoaderCallback(appsId, true);
        MemcacheService appsContent = getMemcacheService("apps-content." + appsId);
        HTTPResponseContent content = (HTTPResponseContent) loadIfNotExisting(appsContent, "/", callback);
        serveContent(content, request, response);
        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/apps/{appsId}/**", method = RequestMethod.GET)
    protected ModelAndView fetchAppContent(HttpServletRequest request, HttpServletResponse response,
                                           @PathVariable("appsId") String appsId) throws Exception {
        String contentUri = substringAfterLast(request.getRequestURI(), appsId);
        AppsContentMemcacheLoaderCallback callback = new AppsContentMemcacheLoaderCallback(appsId);
        MemcacheService appsContent = getMemcacheService("apps-content." + appsId);
        HTTPResponseContent content = (HTTPResponseContent) loadIfNotExisting(appsContent, contentUri, callback);
        serveContent(content, request, response);
        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/apps", method = RequestMethod.POST)
    protected ModelAndView save(HttpServletRequest request, @RequestParam("app") JSONObject app) throws Exception {
        checkAdmin(request);

        URL url = new URL(app.getString("url"));

        Entity e = new Entity(createKey("apps-content", app.getString("id")));
        e.setProperty("label", app.getString("label"));
        e.setProperty("url", url.toString());
        if (app.has("description")) e.setProperty("description", app.getString("description"));
        if (app.has("logo")) e.setProperty("logo", app.getString("logo"));
        if (app.has("homepage")) e.setProperty("homepage", app.getString("homepage"));

        inTransaction(datastore, new PutEntityTransactionCallback(e));
        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/apps/refresh", method = RequestMethod.POST)
    protected ModelAndView refreshContent(HttpServletRequest request) throws Exception {
        checkAdmin(request);

        log.info("clearing apps content cache");
        getMemcacheService().clearAll();
        return new ModelAndView(new OkResponseView());
    }
}
