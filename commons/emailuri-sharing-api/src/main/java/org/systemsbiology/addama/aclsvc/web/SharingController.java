/*
**    Copyright (C) 2003-2010 Institute for Systems Biology
**                            Seattle, Washington, USA.
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
*/
package org.systemsbiology.addama.aclsvc.web;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.systemsbiology.addama.aclsvc.service.AddamaAclDto;
import org.systemsbiology.addama.aclsvc.service.AddamaAclService;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.views.OkResponseView;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author hrovira
 */

public class SharingController extends AbstractController {
    private static final Logger log = Logger.getLogger(SharingController.class.getName());
    
    @Autowired
    private AddamaAclService addamaAclService;

    public void setAddamaAclService(AddamaAclService addamaAclService) {
        this.addamaAclService = addamaAclService;
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = request.getRequestURI();
        log.info("uri=" + requestUri);

        if (!isMatchingScheme(requestUri)) {
            throw new InvalidSyntaxException(requestUri);
        }

        String userEmail = getUserEmail(request);
        log.info("user=" + userEmail);

        if (isOwner(requestUri, userEmail)) {
            return new ModelAndView(new OkResponseView());
        }

        if (isShared(requestUri, userEmail)) {
            return new ModelAndView(new OkResponseView());
        }

        throw new ForbiddenAccessException(userEmail);
    }

    /*
     * Private Methods
     */

    private boolean isMatchingScheme(String actualUri) {
        // The canonical scheme we're looking for here is: /addama/sharing/<path>/<email>/<path>

        // @note Our strategy here is to perform naive parsing of the email and path parts,
        //       then use JavaMail's RFC5322 Internet Address parsing library for verification.
        return (isValidEmail(getEmailPart(actualUri)));
    }

    private String getUserEmail(HttpServletRequest request) {
        return StringUtils.substringAfterLast(request.getHeader("x-addama-registry-user"), "/addama/users/");
    }

    private String getServiceUri(HttpServletRequest request) {
        return request.getHeader("x-addama-service-uri");
    }

    private boolean isOwner(String actualUri, String userEmail) {
        return userEmail.toLowerCase().equals(
                getEmailPart(actualUri).toLowerCase());
    }

    private boolean isShared(String targetUri, String userEmail) {
        try {
            targetUri = StringUtils.chomp(targetUri, "/");
            if (targetUri.endsWith("/read")) {
                String endpoint = StringUtils.substringBetween(targetUri, "/addama/sharing", "/read");
                return addamaAclService.isAllowed(endpoint, new AddamaAclDto("READ", "UserByEmail", userEmail));
            }
            if (targetUri.endsWith("/write")) {
                String endpoint = StringUtils.substringBetween(targetUri, "/addama/sharing", "/write");
                return addamaAclService.isAllowed(endpoint, new AddamaAclDto("WRITE", "UserByEmail", userEmail));
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        return false;
    }

    private boolean isValidEmail(String email) {
        try {
            InternetAddress.parse(email, true);
            return email.contains("@"); //! @note: javax.mail parses to RFC822, and allows addresses without the domain
                                        //!         part. Thus, this additional check is required. If the email address
                                        //!         parsed as valid, but lacks an @ character, it _must_ have parsed
                                        //!         without the domain component. This is not valid for our purposes.
        } catch (AddressException e) {
            log.warning("not a valid email [" + email + "]");
        }
        return false;
    }

    // Retrieves the first email address in the string
    private String getEmailPart(String actualUri) {
        Pattern scheme = Pattern.compile("[^@]+/([^/]*@[^/]*\\.[^/]*)/.+");
        Matcher matcher = scheme.matcher(actualUri);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return "";
    }
}
