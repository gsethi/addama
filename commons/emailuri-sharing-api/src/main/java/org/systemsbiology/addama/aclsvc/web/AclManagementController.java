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
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.aclsvc.service.AddamaAclDto;
import org.systemsbiology.addama.aclsvc.service.AddamaAclService;
import org.systemsbiology.addama.aclsvc.service.AddamaAclSetDto;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;
import org.systemsbiology.addama.commons.web.views.OkResponseView;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

/**
 * @author trobinso (adapted code from hrovira)
 */
// TODO : Check that user is actually owner of the uri before setting acls
@Controller
public class AclManagementController {
    private static final Logger log = Logger.getLogger(AclManagementController.class.getName());

    // @todo Validate the permission of the requesting party (user)
    @Autowired
    private AddamaAclService addamaAclService;

    public void setAddamaAclService(AddamaAclService addamaAclService) {
        this.addamaAclService = addamaAclService;
    }

    @RequestMapping(value = "/**/check"/*, method = RequestMethod.POST*/)
    public ModelAndView checkAcls(HttpServletRequest request,
                                  @RequestParam("acls") String acls) throws Exception {
        log.info("checkAcls(" + request.getRequestURI() + ")");

        JSONObject json = new JSONObject(acls);
        AddamaAclSetDto setDto = AddamaAclSetDto.fromJSONString(json.toString());
        String endpoint = getResourceUri(request, "/check");

        for (AddamaAclDto chkDto : setDto.getEntries()) {
            if (!addamaAclService.isAllowed(endpoint, chkDto)) {
                throw new ForbiddenAccessException("This ACL is not allowed: "
                        + "Permission: " + chkDto.getPermission()
                        + "Scope: " + chkDto.getScope()
                        + "Credential: " + chkDto.getCredential());
            }
        }

        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/**/update"/*, method = RequestMethod.POST*/)
    public ModelAndView updateAcls(HttpServletRequest request,
                                   @RequestParam("acls") String acls) throws Exception {
        log.info("updateAcls(" + request.getRequestURI() + ")");

        JSONObject json = new JSONObject(acls);
        AddamaAclSetDto setDto = AddamaAclSetDto.fromJSONString(json.toString());
        String endpoint = getResourceUri(request, "/update");

        for (AddamaAclDto upDto : setDto.getEntries()) {
            if (!addamaAclService.setAcl(endpoint, upDto)) {
                throw new ForbiddenAccessException("This ACL could not be updated: "
                        + "Permission: " + upDto.getPermission()
                        + "Scope: " + upDto.getScope()
                        + "Credential: " + upDto.getCredential());
            }
        }

        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/**/delete"/*, method = RequestMethod.POST*/)
    public ModelAndView deleteAcls(HttpServletRequest request,
                                   @RequestParam("acls") String acls) throws Exception {
        log.info("deleteAcls(" + request.getRequestURI() + ")");

        JSONObject json = new JSONObject(acls);
        AddamaAclSetDto setDto = AddamaAclSetDto.fromJSONString(json.toString());
        String endpoint = getResourceUri(request, "/delete");

        for (AddamaAclDto remDto : setDto.getEntries()) {
            if (!addamaAclService.removeAcl(endpoint, remDto)) {
                throw new ForbiddenAccessException("This ACL could not be removed: "
                        + "Permission: " + remDto.getPermission()
                        + "Scope: " + remDto.getScope()
                        + "Credential: " + remDto.getCredential());
            }
        }

        return new ModelAndView(new OkResponseView());
    }

    /*
    * Private Methods
    */

    private String getUser(HttpServletRequest request) {
        // TODO : Load user groups, verify access level
        String user = request.getHeader("x-addama-registry-user");
        log.info("getUser(" + request.getRequestURI() + "):" + user);
        return StringUtils.substringAfterLast(user, "/addama/users");
    }

    protected String getResourceUri(HttpServletRequest request, String suffix) {
        String resourceUri = StringUtils.chomp(StringUtils.substringBefore(request.getRequestURI(), suffix), "/");
        log.fine("getResourceUri():" + resourceUri);
        return resourceUri;
    }
}
