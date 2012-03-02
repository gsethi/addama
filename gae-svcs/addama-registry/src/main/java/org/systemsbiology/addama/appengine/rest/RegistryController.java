package org.systemsbiology.addama.appengine.rest;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.systemsbiology.addama.appengine.datastore.PutEntityTransactionCallback;
import org.systemsbiology.addama.commons.web.editors.JSONObjectPropertyEditor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static java.util.UUID.randomUUID;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.systemsbiology.addama.appengine.datastore.DatastoreServiceTemplate.inTransaction;
import static org.systemsbiology.addama.appengine.util.Registry.*;
import static org.systemsbiology.addama.appengine.util.Users.checkAdmin;

/**
 * @author hrovira
 */
@Controller
public class RegistryController {
    private static final Logger log = Logger.getLogger(RegistryController.class.getName());

    private final DatastoreService datastore = getDatastoreService();

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(JSONObject.class, new JSONObjectPropertyEditor());
    }

    @RequestMapping(value = "/registry", method = RequestMethod.POST)
    public void register(HttpServletRequest request, HttpServletResponse response,
                         @RequestParam("registration") JSONObject registration) throws Exception {
        checkAdmin(request);
        try {
            log.info(registration.toString());

            // TODO : Check ownership of services before proceeding
            URL serviceUrl = new URL(registration.getString("url"));
            String family = registration.getString("family");

            String servicePath = serviceUrl.getPath();
            if (servicePath.startsWith("/")) {
                servicePath = substringAfter(servicePath, "/");
            }

            String serviceId = serviceUrl.getHost() + "." + servicePath;

            clearExistingService(serviceId);

            // TODO : Parent/child relationships

            String registryKey = randomUUID().toString();
            Entity pe = newServiceEntity(serviceId, registryKey, registration);
            inTransaction(datastore, new PutEntityTransactionCallback(pe));

            JSONArray mappings = registration.getJSONArray("mappings");
            for (int i = 0; i < mappings.length(); i++) {
                Entity me = newMappingEntity(serviceId, family, mappings.getJSONObject(i));
                inTransaction(datastore, new PutEntityTransactionCallback(me));
            }

            response.addHeader("x-addama-registry-key", registryKey);
        } catch (Exception e) {
            response.setStatus(SC_BAD_REQUEST);
            response.getWriter().write("Bad Registration");
        }
    }
}
