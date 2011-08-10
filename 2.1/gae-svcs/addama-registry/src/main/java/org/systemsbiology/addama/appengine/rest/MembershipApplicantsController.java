package org.systemsbiology.addama.appengine.rest;

import com.google.appengine.api.users.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;
import org.systemsbiology.addama.commons.web.views.OkResponseView;

import javax.servlet.http.HttpServletRequest;

import static org.apache.commons.lang.StringUtils.substringBetween;
import static org.systemsbiology.addama.appengine.util.Memberships.*;
import static org.systemsbiology.addama.appengine.util.Memberships.Membership.applicant;
import static org.systemsbiology.addama.appengine.util.Users.getCurrentUser;

/**
 * @author hrovira
 *         Membership Applicant Controller - Accessible by logged-in users
 */
@Controller
public class MembershipApplicantsController {

    /**
     * Stores logged-in user as a domain membership applicants
     * URI_SCHEME:[/memberships/domains/applicants]
     *
     * @return ModelAndView
     * @throws ForbiddenAccessException - if user is not logged in
     */
    @RequestMapping(value = "/memberships/domain/applicants", method = RequestMethod.POST)
    public ModelAndView set_applicant() throws ForbiddenAccessException {
        User user = getCurrentUser();
        setDomainMembership(new DomainMember(user.getEmail(), applicant));

        return new ModelAndView(new OkResponseView());
    }

    /**
     * Stores logged-in user as a URI membership applicants
     * URI_SCHEME:[/memberships/uri/<any-uri>/applicants]
     *
     * @param request - HttpServletRequest
     * @return ModelAndView
     * @throws ForbiddenAccessException - if user is not logged in
     */
    @RequestMapping(value = "/memberships/uris/**/applicants", method = RequestMethod.POST)
    public ModelAndView apply_for_access_uri(HttpServletRequest request) throws Exception {
        User user = getCurrentUser();
        String uri = substringBetween(request.getRequestURI(), "/memberships", "/applicants");
        createMemberships(applicant, uri, user.getEmail());

        return new ModelAndView(new OkResponseView());
    }


}
