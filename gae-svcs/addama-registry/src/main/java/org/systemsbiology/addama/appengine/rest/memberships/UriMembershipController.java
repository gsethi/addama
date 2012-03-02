package org.systemsbiology.addama.appengine.rest.memberships;

import com.google.appengine.api.users.User;
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
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBetween;
import static org.systemsbiology.addama.appengine.util.Memberships.Membership.*;
import static org.systemsbiology.addama.appengine.util.Memberships.*;
import static org.systemsbiology.addama.appengine.util.Users.*;
import static org.systemsbiology.addama.commons.web.views.JsonItemsView.getItems;

/**
 * @author hrovira
 */
@Controller
public class UriMembershipController {
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(JSONArray.class, "users", new JSONArrayPropertyEditor());
    }

    /*
     * Moderator Control (Admin-only)
     */

    @RequestMapping(value = "/memberships/uris/**/moderate", method = RequestMethod.POST)
    public ModelAndView moderate_uri_access(HttpServletRequest request, @RequestParam("moderator") String email) throws Exception {
        checkAdmin(request);
        String moderatedUri = substringBetween(request.getRequestURI(), "/memberships/uris", "/moderate");
        setModerator(new ModeratedItem(moderatedUri, email));

        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/memberships/uris/**/release", method = RequestMethod.POST)
    public ModelAndView release_uri_access(HttpServletRequest request) throws Exception {
        checkAdmin(request);
        String moderatedUri = substringBetween(request.getRequestURI(), "/memberships/uris", "/release");
        revokeModeration(moderatedUri);

        return new ModelAndView(new OkResponseView());
    }

    /*
    * ModeratedItem URI Control (Admin or Moderator)
    */

    @RequestMapping(value = "/memberships/uris", method = RequestMethod.GET)
    public ModelAndView uris_membership(HttpServletRequest request) throws Exception {
        if (isAdministrator(request)) {
            return new ModelAndView(new JsonItemsView()).addObject("json", getItems(getModeratedItems()));
        }

        String loggedInUser = getLoggedInUserEmail(request);
        return new ModelAndView(new JsonItemsView()).addObject("json", getItems(getModeratedItems(loggedInUser)));
    }

    @RequestMapping(value = "/memberships/uris/**", method = RequestMethod.GET)
    public ModelAndView membership_uri(HttpServletRequest request) throws Exception {
        String uri = substringAfter(request.getRequestURI(), "/memberships/uris");

        checkAdminOrModerator(request, uri);
        return new ModelAndView(new JsonItemsView()).addObject("json", getItems(getModeratedUsers(uri)));
    }

    @RequestMapping(value = "/memberships/uris/**", method = RequestMethod.POST)
    public ModelAndView set_uri_access(HttpServletRequest request,
                                       @RequestParam(value = "everyoneIsGuest", required = false) boolean everyoneIsGuest,
                                       @RequestParam(value = "guests", required = false) String[] guests,
                                       @RequestParam(value = "members", required = false) String[] members) throws Exception {
        String uri = substringAfter(request.getRequestURI(), "/memberships/uris");
        checkAdminOrModerator(request, uri);
        if (everyoneIsGuest) {
            createMemberships(guest, uri, everyone.name());
        } else {
            revokeMemberships(uri, everyone.name());
        }
        createMemberships(guest, uri, guests);
        createMemberships(member, uri, members);

        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/memberships/uris/**/revoke", method = RequestMethod.POST)
    public ModelAndView revoke_uri_access(HttpServletRequest request,
                                          @RequestParam(value = "guests", required = false) String[] guests,
                                          @RequestParam(value = "members", required = false) String[] members) throws Exception {
        String uri = substringBetween(request.getRequestURI(), "/memberships/uris", "/revoke");
        checkAdminOrModerator(request, uri);
        List<String> revokeds = new ArrayList<String>();
        if (guests != null) revokeds.addAll(asList(guests));
        if (members != null) revokeds.addAll(asList(members));

        if (!revokeds.isEmpty()) {
            revokeMemberships(uri, revokeds.toArray(new String[revokeds.size()]));
        } else {
            revokeMemberships(uri, everyone.name());
        }

        return new ModelAndView(new OkResponseView());
    }

    /*
     * Members
     */

    @RequestMapping(value = "/memberships/mine", method = RequestMethod.GET)
    public ModelAndView memberships_mine() throws Exception {
        User user = getCurrentUser();

        JSONObject json = getItems(myMemberships(user.getEmail()));
        json.put("domainMembership", domainMembership(user.getEmail()));

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    /*
    * Applicants
    */

    /*
    * Private Methods
    */

    private void checkAdminOrModerator(HttpServletRequest request, String uri)
            throws ForbiddenAccessException, ResourceNotFoundException {
        if (isAdministrator(request)) {
            return;
        }

        ModeratedItem moderatedItem = getModeratedItem(uri);
        if (moderatedItem == null) {
            throw new ResourceNotFoundException(uri);
        }

        String loggedInUser = getLoggedInUserEmail(request);
        if (moderatedItem.isModerator(loggedInUser)) {
            return;
        }

        throw new ForbiddenAccessException(loggedInUser);
    }
}
