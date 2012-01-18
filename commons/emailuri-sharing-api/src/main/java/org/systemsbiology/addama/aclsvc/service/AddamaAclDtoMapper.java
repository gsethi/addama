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
package org.systemsbiology.addama.aclsvc.service;

import org.systemsbiology.addama.aclsvc.dao.AddamaAcl;
import org.systemsbiology.addama.aclsvc.service.AddamaAclDto;


/**
 * @author trobinso (adapted from the MRMAtlas project)
 */
public class AddamaAclDtoMapper {
    public static AddamaAclDto dtoFromAddamaAcl(AddamaAcl acl) {
        AddamaAclDto dto = new AddamaAclDto();
        dto.setPermission(acl.getPermission().toString());
        dto.setScope(acl.getScope().toString());
        dto.setCredential(acl.getCredential());

        return dto;
    }

    public static AddamaAcl addamaAclFromDto(AddamaAclDto dto) {
        AddamaAcl acl = new AddamaAcl();
        acl.setPermission(AddamaAcl.AddamaAclPermission.valueOf(dto.getPermission()));
        acl.setScope(AddamaAcl.AddamaAclScope.valueOf(dto.getScope()));
        acl.setCredential(dto.getCredential());

        return acl;
    }
}
