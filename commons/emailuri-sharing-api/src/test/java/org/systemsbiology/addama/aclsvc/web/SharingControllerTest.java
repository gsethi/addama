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

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;

import static org.junit.Assert.assertEquals;

/**
 * @author hrovira
 */
public class SharingControllerTest {
    private final static String ME = "me@isb.org";
    private final static String YOU = "you@isb.org";

    private SharingController controller;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Before
    public void setUp() {
        controller = new SharingController();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.addHeader("x-addama-registry-user", "/addama/users/" + ME);
    }

    @Test
    public void pass_read() throws Exception {
        request.setRequestURI("/emailuri-sharing-svc/addama/sharing/a/b/" + ME + "/d/read");
        controller.handleRequestInternal(request, response);
        assertEquals(MockHttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    public void pass_write() throws Exception {
        request.setRequestURI("/emailuri-sharing-svc/addama/sharing/a/b/" + ME + "/d/read");
        controller.handleRequestInternal(request, response);
        assertEquals(MockHttpServletResponse.SC_OK, response.getStatus());
    }

    @Test(expected = ForbiddenAccessException.class)
    public void forbidden_read() throws Exception {
        request.setRequestURI("/emailuri-sharing-svc/addama/sharing/a/b/" + YOU + "/d/read");
        controller.handleRequestInternal(request, response);
    }

    @Test(expected = ForbiddenAccessException.class)
    public void forbidden_write() throws Exception {
        request.setRequestURI("/emailuri-sharing-svc/addama/sharing/a/b/" + YOU + "/d/read");
        controller.handleRequestInternal(request, response);
    }

    @Test(expected = InvalidSyntaxException.class)
    public void unknown_read() throws Exception {
        request.setRequestURI("/emailuri-sharing-svc/addama/sharing/a/b/c/d/read");
        controller.handleRequestInternal(request, response);
    }

    @Test(expected = InvalidSyntaxException.class)
    public void unknown_write() throws Exception {
        request.setRequestURI("/emailuri-sharing-svc/addama/sharing/a/b/c/d/read");
        controller.handleRequestInternal(request, response);
    }

    @Test(expected = InvalidSyntaxException.class)
    public void bademail_read() throws Exception {
        request.setRequestURI("/emailuri-sharing-svc/addama/sharing/a/b/@d/d/read");
        controller.handleRequestInternal(request, response);
    }

    @Test(expected = InvalidSyntaxException.class)
    public void bademail_write() throws Exception {
        request.setRequestURI("/emailuri-sharing-svc/addama/sharing/a/b/@d/d/read");
        controller.handleRequestInternal(request, response);
    }
}
