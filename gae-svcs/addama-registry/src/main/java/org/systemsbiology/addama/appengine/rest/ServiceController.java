package org.systemsbiology.addama.appengine.rest;

import com.google.appengine.api.datastore.*;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
import static org.apache.commons.lang.StringUtils.substringAfterLast;

/**
 * @author hrovira
 */
@Controller
public class ServiceController {
    private static final Logger log = Logger.getLogger(ServiceController.class.getName());

    private static final DatastoreService datastore = getDatastoreService();

    @RequestMapping(value = "/services", method = RequestMethod.GET)
    public ModelAndView list() throws Exception {
        log.info("list");

        JSONObject json = new JSONObject();
        json.put("uri", "/addama/services");

        PreparedQuery pq = datastore.prepare(new Query("registry-services"));
        Iterator<Entity> itr = pq.asIterator();
        while (itr.hasNext()) {
            Entity e = itr.next();
            String id = e.getKey().getName();

            JSONObject item = new JSONObject();
            item.put("id", id);
            item.put("uri", "/addama/services/" + id);
            item.put("label", e.getProperty("label").toString());
            item.put("url", e.getProperty("url").toString());
            json.append("items", item);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }


    @RequestMapping(value = "/services/*", method = RequestMethod.GET)
    public ModelAndView service(HttpServletRequest request) throws Exception {
        String serviceUri = request.getRequestURI();
        String serviceId = substringAfterLast(serviceUri, "services/");
        log.info(serviceId);

        JSONObject json = new JSONObject();
        json.put("id", serviceId);
        json.put("uri", serviceUri);

        try {
            Entity e = datastore.get(createKey("registry-services", serviceId));
            json.put("label", e.getProperty("label").toString());
            json.put("url", e.getProperty("url").toString());
        } catch (EntityNotFoundException ex) {
            log.warning(ex.getMessage());
            throw new ResourceNotFoundException(serviceUri);
        }

        PreparedQuery pq = datastore.prepare(new Query("registry-services").addFilter("service", EQUAL, serviceId));
        for (Entity e : pq.asIterable()) {
            JSONObject mapping = new JSONObject();
            mapping.put("id", e.getKey().getName());
            if (e.hasProperty("family")) mapping.put("family", e.getProperty("family").toString());
            if (e.hasProperty("uri")) mapping.put("uri", e.getProperty("uri").toString());
            if (e.hasProperty("label")) mapping.put("label", e.getProperty("label").toString());
            json.append("items", mapping);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

}
