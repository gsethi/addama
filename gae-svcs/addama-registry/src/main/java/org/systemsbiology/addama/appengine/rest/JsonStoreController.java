package org.systemsbiology.addama.appengine.rest;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.appengine.editors.JSONObjectPropertyEditor;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.chomp;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.systemsbiology.addama.appengine.util.JsonStore.*;
import static org.systemsbiology.addama.appengine.util.Users.checkAdmin;

/**
 * @author hrovira
 */
public class JsonStoreController {
    private static final Logger log = Logger.getLogger(JsonStoreController.class.getName());

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(JSONObject.class, new JSONObjectPropertyEditor());
    }

    /*
    * Controllers
    */
    @RequestMapping(value = "/stores", method = GET)
    protected ModelAndView listStores(HttpServletRequest request) throws Exception {
        JSONObject json = new JSONObject();
        json.put("uri", chomp(request.getRequestURI(), "/"));
        appendItems(json, retrieveStores());
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/stores/{storeId}", method = GET)
    protected ModelAndView listStore(HttpServletRequest request,
                                     @PathVariable("storeId") String storeId) throws Exception {
        JSONObject json = new JSONObject();
        json.put("uri", chomp(request.getRequestURI(), "/"));
        json.put("dataSchema", retrieveSchema(storeId));
        appendItems(json, retrieveItems(storeId));
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/stores/{storeId}", method = POST)
    protected ModelAndView storeStore(HttpServletRequest request, @PathVariable("storeId") String storeId,
                                      @RequestParam("store") JSONObject store) throws Exception {
        checkAdmin(request);

        if (!store.has("label")) {
            throw new InvalidSyntaxException("store must be labeled");
        }

        createStore(storeId, store);

        JSONObject json = new JSONObject();
        json.put("id", storeId);
        json.put("uri", chomp(request.getRequestURI(), "/") + "/" + storeId);
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/stores/{storeId}/{itemId}", method = GET)
    protected ModelAndView listItem(HttpServletRequest request, @PathVariable("storeId") String storeId,
                                    @PathVariable("itemId") String itemId) throws Exception {
        log.info(storeId + ":" + itemId);

        JSONObject json = retrieveItem(storeId, itemId);
        json.put("uri", request.getRequestURI());

        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/stores/{storeId}", method = POST)
    protected ModelAndView create(HttpServletRequest request, @PathVariable("storeId") String storeId,
                                  @RequestParam("item") JSONObject item) throws Exception {
        checkAdmin(request);

        if (!item.has("label")) {
            throw new InvalidSyntaxException("items must be labeled");
        }

        UUID itemId = createItem(storeId, item);
        String id = itemId.toString();
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("uri", chomp(request.getRequestURI(), "/") + "/" + id);
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/stores/{storeId}/{itemId}", method = POST)
    protected ModelAndView update(HttpServletRequest request,
                                  @PathVariable("storeId") String storeId, @PathVariable("itemId") String itemId,
                                  @RequestParam("item") JSONObject item) throws Exception {
        checkAdmin(request);
        updateItem(storeId, itemId, item);
        return new ModelAndView(new OkResponseView());
    }


    @RequestMapping(value = "/stores/{storeId}/{itemId}/delete", method = POST)
    protected ModelAndView delete(HttpServletRequest request, @PathVariable("storeId") String storeId,
                                  @PathVariable("itemId") String itemId) throws Exception {
        checkAdmin(request);
        deleteItem(storeId, itemId);
        return new ModelAndView(new OkResponseView());
    }

    /*
     * Private Methods
     */
    private void appendItems(JSONObject json, Iterable<JSONObject> items) throws JSONException {
        String baseUri = json.getString("uri");
        for (JSONObject item : items) {
            item.put("uri", baseUri + "/" + item.getString("id"));
            json.append("items", item);
        }
    }
}
