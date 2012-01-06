package org.systemsbiology.addama.appengine.rest;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.PutEntityTransactionCallback;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.util.ArrayList;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static java.util.UUID.randomUUID;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.systemsbiology.addama.appengine.util.Registry.*;
import static org.systemsbiology.addama.appengine.util.Users.checkAdmin;
import static org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate.inTransaction;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.asPackage;

/**
 * @author hrovira
 */
@Controller
public class RegistryController {
    private final DatastoreService datastore = getDatastoreService();

    @RequestMapping(value = "/registry", method = RequestMethod.POST)
    public void register(HttpServletRequest request, HttpServletResponse response,
                         @RequestParam("registration") String registration) throws Exception {
        checkAdmin(request);
        try {
            JSONObject json = new JSONObject(registration);

            // TODO : Check ownership of services before proceeding

            String serviceId = asPackage(new URL(json.getString("url")));
            clearExistingService(serviceId);

            ArrayList<Entity> entities = new ArrayList<Entity>();

            String registryKey = randomUUID().toString();
            entities.add(newServiceEntity(serviceId, registryKey, json));

            JSONArray mappings = json.getJSONArray("mappings");
            for (int i = 0; i < mappings.length(); i++) {
                entities.add(newMappingEntity(serviceId, mappings.getJSONObject(i)));
            }

            inTransaction(datastore, new PutEntityTransactionCallback(entities));

            response.addHeader("x-addama-registry-key", registryKey);
        } catch (Exception e) {
            response.setStatus(SC_BAD_REQUEST);
            response.getWriter().write("Bad Registration");
        }
    }
}