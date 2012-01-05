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

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.systemsbiology.addama.aclsvc.dao.AddamaAcl;
import org.systemsbiology.addama.aclsvc.dao.AddamaAcl.AddamaAclPermission;
import org.systemsbiology.addama.aclsvc.dao.AddamaAcl.AddamaAclScope;
import org.systemsbiology.addama.aclsvc.service.AddamaAclDto;
import org.systemsbiology.addama.aclsvc.service.AddamaAclDtoMapper;
import org.systemsbiology.addama.aclsvc.service.AddamaAclSetDto;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author trobinso
 */
@ContextConfiguration(locations = {"/gaeacl-test.xml"})
public class AddamaAclSetDtoTest {
    private static final String OWNER = "John Smith";
    private static final String CREDENTIAL = "My Credential";

    @Test
    public void testDtoConversionLayer() throws Exception {
        // Validate simple conversion to and from a dto object
        // Note this only validates the invariant conditions of a symmetric conversion, not each method individually
        for (AddamaAclPermission perm : AddamaAclPermission.values()) {
            for (AddamaAclScope scope : AddamaAclScope.values()) {
                AddamaAcl acl = new AddamaAcl();
                acl.setPermission(perm);
                acl.setScope(scope);
                acl.setCredential(CREDENTIAL);
                AddamaAcl convertedAcl = AddamaAclDtoMapper.addamaAclFromDto(AddamaAclDtoMapper.dtoFromAddamaAcl(acl));
                assertEquals(acl.getCredential(), convertedAcl.getCredential());
                assertEquals(acl.getScope(), convertedAcl.getScope());
                assertEquals(acl.getPermission(), convertedAcl.getPermission());
            }
        }
    }

    @Test
    public void testJsonConversionLayer() throws Exception {

        // Generate a simple, representative list of ACLs
        AddamaAclSetDto dto = new AddamaAclSetDto();
        List<AddamaAclDto> dtoList = new ArrayList<AddamaAclDto>();
        for (AddamaAclPermission perm : AddamaAclPermission.values()) {
            for (AddamaAclScope scope : AddamaAclScope.values()) {
                AddamaAcl acl = new AddamaAcl();
                acl.setPermission(perm);
                acl.setScope(scope);
                acl.setCredential(CREDENTIAL);
                dtoList.add(AddamaAclDtoMapper.dtoFromAddamaAcl(acl));
            }
        }

        // Run the full conversion process
        AddamaAclSetDto initial = AddamaAclSetDto.fromAclDefinition(OWNER, dtoList);
        JSONObject json = initial.toJSONObject();
        AddamaAclSetDto test = AddamaAclSetDto.fromJSONString(json.toString());

        // Test the results

        // Implied tests:
        //  assertNotNull(initial);
        //  assertNotNull(json);

        // Remaining test cases:
        assertNotNull(test);
        assertEquals(initial.toJSONString(), test.toJSONString());
    }
}

