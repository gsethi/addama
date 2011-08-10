package org.systemsbiology.addama.coresvcs.gae.controllers;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.PutEntityTransactionCallback;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate.inTransaction;

/**
 * @author hrovira
 */
@Controller
public class RegistryController {
    private static final Logger log = Logger.getLogger(RegistryController.class.getName());

    private final DatastoreService datastore = getDatastoreService();

    @RequestMapping(value = "/registry/services/**", method = RequestMethod.POST)
    public void setService(HttpServletRequest request, HttpServletResponse response,
                           @RequestParam("service") String service) throws Exception {
        log.info(request.getRequestURI());

        String serviceUri = "/addama/services" + substringAfterLast(request.getRequestURI(), "/registry/services");
        JSONObject json = new JSONObject(service);

        Entity e = new Entity(createKey("registry-services", serviceUri));
        e.setProperty("url", new URL(json.getString("url")).toString());
        if (json.has("label")) {
            e.setProperty("label", json.getString("label"));
        }
        if (json.has("searchable")) {
            e.setProperty("searchable", json.getBoolean("searchable"));
        }
        if (json.has("sharing")) {
            e.setProperty("sharing", json.getString("sharing"));
        }

        String uuid = randomUUID().toString();
        e.setUnindexedProperty("REGISTRY_SERVICE_KEY", uuid);
        inTransaction(datastore, new PutEntityTransactionCallback(e));

        response.addHeader("x-addama-registry-key", uuid);
    }

    @RequestMapping(value = "/registry/mappings/**", method = RequestMethod.POST)
    public void setMapping(HttpServletRequest request, HttpServletResponse response,
                           @RequestParam("mapping") String mapping) throws Exception {
        log.fine(request.getRequestURI() + ":" + mapping);

        String mappingUri = substringAfterLast(request.getRequestURI(), "/registry/mappings");
        JSONObject json = new JSONObject(mapping);

        String uri = json.getString("uri");
        if (!json.has("family")) {
            String familyName = substringBetween(uri, "/addama/", "/");
            if (!isEmpty(familyName)) {
                json.put("family", "/addama/" + familyName);
            }
        }
        if (!json.has("pattern")) {
            json.put("pattern", uri + "/**");
        }

        Entity e = new Entity(createKey("registry-mappings", mappingUri));
        for (String required : new String[]{"service", "pattern", "uri"}) {
            e.setProperty(required, json.getString(required));
        }

        for (String optional : new String[]{"label", "family"}) {
            if (json.has(optional)) {
                e.setProperty(optional, json.getString(optional));
            }
        }

        for (String optBoolean : new String[]{"staticContent", "handleAsynch"}) {
            e.setProperty(optBoolean, json.optBoolean(optBoolean, false));
        }

        inTransaction(datastore, new PutEntityTransactionCallback(e));

        response.addHeader("x-addama-success", "true");
    }
}
