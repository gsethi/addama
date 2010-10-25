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
package org.systemsbiology.addama.sharingsvc.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class CheckAccessController {
    private static final Logger log = Logger.getLogger(CheckAccessController.class.getName());

    @RequestMapping("/**/retrieve")
    public void retrieve(HttpServletRequest request, HttpServletResponse response) {
        log.fine("retrieve(" + request.getRequestURI() + ")");
        checkUser(request);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @RequestMapping("/**/update")
    public void update(HttpServletRequest request, HttpServletResponse response) {
        log.fine("update(" + request.getRequestURI() + ")");
        checkUser(request);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @RequestMapping("/**/delete")
    public void delete(HttpServletRequest request, HttpServletResponse response) {
        log.fine("delete(" + request.getRequestURI() + ")");
        checkUser(request);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /*
    * Private Methods
    */

    private void checkUser(HttpServletRequest request) {
        // TODO : Load user groups, verify access level
        String user = request.getHeader("x-addama-registry-user");
        log.fine("checkUser(" + request.getRequestURI() + "):" + user);
    }
}
