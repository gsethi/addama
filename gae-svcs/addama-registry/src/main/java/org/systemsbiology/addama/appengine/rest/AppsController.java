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
import org.systemsbiology.addama.appengine.editors.JSONObjectPropertyEditor;
import org.systemsbiology.addama.appengine.pojos.HTTPResponseContent;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
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
    //    private final MemcacheService appsContent = getMemcacheService("apps-content");
    private DatastoreService datastore = getDatastoreService();

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
            json.append("items", item);
        }
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/apps/homepage", method = RequestMethod.GET)
    protected ModelAndView homepage() throws Exception {
        JSONObject json = new JSONObject();

        PreparedQuery pq = datastore.prepare(new Query("apps-content").addFilter("homepage", EQUAL, true));
        Iterator itr = pq.asIterator();
        if (itr.hasNext()) {
            Entity e = (Entity) itr.next();
            JSONObject item = new JSONObject();
            String id = e.getKey().getName();
            item.put("id", id);
            item.put("uri", "/addama/apps/" + id);
            item.put("label", e.getProperty("label").toString());
            item.put("url", e.getProperty("url").toString());
            return new ModelAndView(new JsonView()).addObject("json", json);
        }
        throw new ResourceNotFoundException("/apps/homepage");
    }

    @RequestMapping(value = "/apps/{appsId}", method = RequestMethod.GET)
    protected ModelAndView fetchApp(HttpServletRequest request, HttpServletResponse response,
                                    @PathVariable("appsId") String appsId) throws Exception {
        log.info(appsId);
        AppsContentMemcacheLoaderCallback callback = new AppsContentMemcacheLoaderCallback(appsId);
        MemcacheService appsContent = getMemcacheService("apps-content." + appsId);
        HTTPResponseContent content = (HTTPResponseContent) loadIfNotExisting(appsContent, "/index.html", callback);
        if (content == null) {
            content = (HTTPResponseContent) loadIfNotExisting(appsContent, "/", callback);
        }
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

        boolean homepage = app.optBoolean("homepage", false);

        Entity e = new Entity(createKey("apps-content", app.getString("id")));
        e.setProperty("label", app.getString("label"));
        e.setProperty("url", url.toString());
        e.setProperty("homepage", homepage);

        if (homepage) {
            PreparedQuery pq = datastore.prepare(new Query("apps-content").addFilter("homepage", EQUAL, true));
            for (Entity existing : pq.asIterable()) {
                existing.setProperty("homepage", false);
                inTransaction(datastore, new PutEntityTransactionCallback(existing));
            }
        }

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
