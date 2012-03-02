package org.systemsbiology.addama.appengine.rest.memberships;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.editors.JSONArrayPropertyEditor;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;

import javax.servlet.http.HttpServletRequest;

import static org.systemsbiology.addama.appengine.util.Memberships.*;
import static org.systemsbiology.addama.appengine.util.Users.checkAdmin;
import static org.systemsbiology.addama.commons.web.views.JsonItemsView.appendItems;

/**
 * @author hrovira
 *         Domain Membership Controller - for Admin Only Access
 */
@Controller
public class DomainMembershipController {
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(JSONArray.class, new JSONArrayPropertyEditor());
    }


    @RequestMapping(value = "/memberships/domain", method = RequestMethod.GET)
    public ModelAndView domain_access(HttpServletRequest request) throws Exception {
        checkAdmin(request);

        JSONObject json = new JSONObject();
        json.put("uri", "/addama/memberships/domain");
        json.put("enabled", isDomainMembershipActivated());
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/memberships/domain", method = RequestMethod.POST)
    public ModelAndView set_domain_access(HttpServletRequest request, @RequestParam("enabled") boolean enabled) throws Exception {
        checkAdmin(request);
        activateDomainMembership(enabled);

        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/memberships/domain/users", method = RequestMethod.GET)
    public ModelAndView domain_users(HttpServletRequest request) throws Exception {
        checkAdmin(request);

        JSONObject json = new JSONObject();
        json.put("uri", "/addama/memberships/domain/users");
        json.put("enabled", isDomainMembershipActivated());
        appendItems(json, domainMembers());
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/memberships/domain/users", method = RequestMethod.POST)
    public ModelAndView set_domain_users(HttpServletRequest request, @RequestParam("users") JSONArray users) throws Exception {
        checkAdmin(request);

        DomainMember[] members = new DomainMember[users.length()];
        for (int i = 0; i < users.length(); i++) {
            members[i] = new DomainMember(users.getJSONObject(i));
        }
        setDomainMembership(members);

        return new ModelAndView(new OkResponseView());
    }
}
