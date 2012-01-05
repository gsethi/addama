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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.aclsvc.dao.AddamaAcl;
import org.systemsbiology.addama.aclsvc.dao.AddamaAcl.AddamaAclPermission;
import org.systemsbiology.addama.aclsvc.dao.AddamaAcl.AddamaAclScope;
import org.systemsbiology.addama.aclsvc.service.AddamaAclDto;
import org.systemsbiology.addama.aclsvc.service.AddamaAclDtoMapper;
import org.systemsbiology.addama.aclsvc.service.AddamaAclSetDto;
import org.systemsbiology.addama.commons.web.views.ForbiddenAccessView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author trobinso
 */
@ContextConfiguration(locations = {"/gaeacl-test.xml"})
public class AclManagementControllerTest {
    private static final String OWNER = "/addama/users/john@smith.com";
    private static final String PATH = "/my/path";

    private MockHttpServletRequest request;

    @Autowired
    private AclManagementController controller;

    public void setAclManagementController(AclManagementController controller) {
        this.controller = controller;
    }

    @Before
    public void setUp() {
        request = new MockHttpServletRequest();
        request.addHeader("x-addama-registry-user", OWNER);
    }

    @Test
    public void todo() {
        // reinstate these tests once spring dependencies are resolved
    }

    //    @Test(expected = ForbiddenAccessException.class)
    public void testCheckAcls() throws Exception {

        ModelAndView mav = createTestAcls();
        assertNotNull(mav);
        assertEquals(OkResponseView.class, mav.getView().getClass());

        mav = checkTestAcls();
        assertNotNull(mav);
        assertEquals(OkResponseView.class, mav.getView().getClass());

        mav = deleteTestAcls();
        assertNotNull(mav);
        assertEquals(OkResponseView.class, mav.getView().getClass());

        mav = checkTestAcls();
        assertNotNull(mav);
        assertEquals(ForbiddenAccessView.class, mav.getView().getClass());

    }

    //    @Test
    public void testUpdateAcl() throws Exception {
        ModelAndView mav = createTestAcls();
        assertNotNull(mav);
        assertEquals(OkResponseView.class, mav.getView().getClass());
    }

    //    @Test
    public void testDeleteAcl() throws Exception {
        ModelAndView mav = createTestAcls();
        assertNotNull(mav);
        assertEquals(OkResponseView.class, mav.getView().getClass());

        mav = deleteTestAcls();
        assertNotNull(mav);
        assertEquals(OkResponseView.class, mav.getView().getClass());
    }

    /*
     * Helper Methods
     */

    private ModelAndView createTestAcls() throws Exception {
        // Create a simple set of ACLs
        List<AddamaAclDto> entries = new ArrayList<AddamaAclDto>();
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.AllUsers, "")));
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.AllAuthenticatedUsers, "")));
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.GroupById, "")));
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.GroupByEmail, "")));
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.UserById, "")));
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.UserByEmail, "")));

        AddamaAclSetDto setDto = new AddamaAclSetDto(OWNER, entries);

        // Attempt to create the ACLs
        request.setRequestURI(PATH + "/update");
        return controller.updateAcls(request, setDto.toJSONString());
    }

    private ModelAndView checkTestAcls() throws Exception {
        // Create a simple set of ACLs
        List<AddamaAclDto> entries = new ArrayList<AddamaAclDto>();
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.AllUsers, "")));
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.AllAuthenticatedUsers, "")));
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.GroupById, "")));
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.GroupByEmail, "")));
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.UserById, "")));
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.UserByEmail, "")));

        AddamaAclSetDto setDto = new AddamaAclSetDto(OWNER, entries);

        // Attempt to create the ACLs
        request.setRequestURI(PATH + "/check");
        return controller.checkAcls(request, setDto.toJSONString());
    }

    private ModelAndView deleteTestAcls() throws Exception {
        // Create a simple set of ACLs
        List<AddamaAclDto> entries = new ArrayList<AddamaAclDto>();
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.AllUsers, "")));
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.AllAuthenticatedUsers, "")));
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.GroupById, "")));
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.GroupByEmail, "")));
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.UserById, "")));
        entries.add(AddamaAclDtoMapper.dtoFromAddamaAcl(
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.UserByEmail, "")));

        AddamaAclSetDto setDto = new AddamaAclSetDto(OWNER, entries);

        // Attempt to create the ACLs
        request.setRequestURI(PATH + "/delete");
        return controller.deleteAcls(request, setDto.toJSONString());
    }
}
