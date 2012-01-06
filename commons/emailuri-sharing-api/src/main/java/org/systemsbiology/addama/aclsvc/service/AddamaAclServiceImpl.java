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

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.systemsbiology.addama.aclsvc.dao.AddamaAcl;
import org.systemsbiology.addama.aclsvc.dao.AddamaAcl.AddamaAclPermission;
import org.systemsbiology.addama.aclsvc.dao.AddamaAcl.AddamaAclScope;
import org.systemsbiology.addama.aclsvc.dao.AddamaAclDao;
import java.util.ArrayList;
import java.util.List;

// @todo Remove after @Autowiring
import org.systemsbiology.addama.aclsvc.dao.AddamaAclDaoImpl;

/**
 * @author trobinso
 */
public class AddamaAclServiceImpl implements AddamaAclService {

    // @todo Swap this out for an @Autowired implementation, allocating the appropriate configuration information
    @Autowired
    private AddamaAclDao addamaAclDao;

    public void setAddamaAclDao(AddamaAclDaoImpl addamaAclDao) throws Exception {
        this.addamaAclDao = addamaAclDao;
    }

    /*
     * Accessors
     */

    //! (Security note: all accessors should account for, and strive to provide, the minimal amount of
    //!  information in its response, /including/ side effects such as timing and control flow.)

    public boolean isAllowed(String endpoint, AddamaAclDto challenge) throws Exception {
        // Tests if the challenge ACL is allowed for the given endpoint
        AddamaAcl acl = AddamaAclDtoMapper.addamaAclFromDto(challenge);
        AddamaAclPermission allowed = addamaAclDao.getPermission(canonicalize(endpoint), acl.getScope(), acl.getCredential());
        return (allowed.compareTo(acl.getPermission()) >= 0);
    }

    public boolean isOwner(String endpoint, String owner) throws Exception {
        // Checks if this person is the owner
        return owner.equals(addamaAclDao.getOwner(canonicalize(endpoint)));
    }

    public boolean isAllRead(String endpoint) throws Exception {
        return (addamaAclDao.getPermission(canonicalize(endpoint), AddamaAclScope.AllUsers, "")
                == AddamaAclPermission.READ);
    }

    public boolean isAllWrite(String endpoint) throws Exception {
        return (addamaAclDao.getPermission(canonicalize(endpoint), AddamaAclScope.AllUsers, "")
                == AddamaAclPermission.WRITE);
    }

    public boolean isAllFullControl(String endpoint) throws Exception {
        return (addamaAclDao.getPermission(canonicalize(endpoint), AddamaAclScope.AllUsers, "")
                == AddamaAclPermission.FULL_CONTROL);
    }

    public boolean isAllAuthenticatedRead(String endpoint) throws Exception {
        return (addamaAclDao.getPermission(canonicalize(endpoint), AddamaAclScope.AllAuthenticatedUsers, "")
                == AddamaAclPermission.READ);
    }

    public boolean isAllAuthenticatedWrite(String endpoint) throws Exception {
        return (addamaAclDao.getPermission(canonicalize(endpoint), AddamaAclScope.AllAuthenticatedUsers, "")
                == AddamaAclPermission.WRITE);
    }

    public boolean isAllAuthenticatedFullControl(String endpoint) throws Exception {
        return (addamaAclDao.getPermission(canonicalize(endpoint), AddamaAclScope.AllAuthenticatedUsers, "")
                == AddamaAclPermission.FULL_CONTROL);
    }

    public boolean isNoAccess(String endpoint) throws Exception {
        return (!addamaAclDao.hasAccess(canonicalize(endpoint)));
    }

    public String getOwner(String endpoint) throws Exception {
        return addamaAclDao.getOwner(canonicalize(endpoint));
    }

    public List<AddamaAclDto> getAcls(String endpoint) throws Exception {
        List<AddamaAclDto> dtos = new ArrayList<AddamaAclDto>();
        for (AddamaAcl acl: addamaAclDao.getAcls(canonicalize(endpoint))) {
            dtos.add(AddamaAclDtoMapper.dtoFromAddamaAcl(acl));
        }
        return dtos;
    }

    /*
    public List<AddamaAclDto> getReadable(String endpoint) throws Exception {

    }

    public List<AddamaAclDto> getWriteable(String endpoint) throws Exception {

    }

    public List<AddamaAclDto> getFullControl(String endpoint) throws Exception {

    }
    */

    /*
     * Mutators
     */

    public boolean setAcl(String endpoint, AddamaAclDto target) throws Exception {
        return addamaAclDao.updateAcl(canonicalize(endpoint), AddamaAclDtoMapper.addamaAclFromDto(target));
    }

    public boolean setAcls(String endpoint, List<AddamaAclDto> targetList) throws Exception {
        List<AddamaAcl> acls = new ArrayList<AddamaAcl>();
        for (AddamaAclDto aclDto: targetList) {
            acls.add(AddamaAclDtoMapper.addamaAclFromDto(aclDto));
        }
        return addamaAclDao.replaceAcls(canonicalize(endpoint), acls);
    }

    public boolean setOwner(String endpoint, String owner) throws Exception {
        return addamaAclDao.setOwner(canonicalize(endpoint), owner);
    }

    public boolean setAllRead(String endpoint) throws Exception {
        return addamaAclDao.updateAcl(canonicalize(endpoint),
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.AllUsers, ""));
    }

    public boolean setAllWrite(String endpoint) throws Exception {
        return addamaAclDao.updateAcl(canonicalize(endpoint),
                new AddamaAcl(AddamaAclPermission.WRITE, AddamaAclScope.AllUsers, ""));
    }

    public boolean setAllFullControl(String endpoint) throws Exception {
        return addamaAclDao.updateAcl(canonicalize(endpoint),
                new AddamaAcl(AddamaAclPermission.FULL_CONTROL, AddamaAclScope.AllUsers, ""));
    }

    public boolean setAllAuthenticatedRead(String endpoint) throws Exception {
        return addamaAclDao.updateAcl(canonicalize(endpoint),
                new AddamaAcl(AddamaAclPermission.READ, AddamaAclScope.AllAuthenticatedUsers, ""));
    }

    public boolean setAllAuthenticatedWrite(String endpoint) throws Exception {
        return addamaAclDao.updateAcl(canonicalize(endpoint),
                new AddamaAcl(AddamaAclPermission.WRITE, AddamaAclScope.AllAuthenticatedUsers, ""));
    }

    public boolean setAllAuthenticatedFullControl(String endpoint) throws Exception {
        return addamaAclDao.updateAcl(canonicalize(endpoint),
                new AddamaAcl(AddamaAclPermission.FULL_CONTROL, AddamaAclScope.AllAuthenticatedUsers, ""));
    }

    public boolean setNoAccess(String endpoint) throws Exception {
        List<AddamaAcl> acls = new ArrayList<AddamaAcl>();
        AddamaAcl acl = new AddamaAcl(AddamaAclPermission.NO_ACCESS, AddamaAclScope.AllUsers, "");
        acls.add(acl);
        return addamaAclDao.replaceAcls(canonicalize(endpoint), acls);
    }

    public boolean removeAcl(String endpoint, AddamaAclDto target) throws Exception {
        return addamaAclDao.deleteAcl(canonicalize(endpoint), AddamaAclDtoMapper.addamaAclFromDto(target));
    }

    public boolean clearAcls(String endpoint) throws Exception {
        return addamaAclDao.clearAcls(canonicalize(endpoint));
    }

    /*
     * Helper methods
     */
    private String canonicalize(String endpoint) {
        // Canonicalization (C14n) method
        //  Translates the given endpoint mapping to the correct form before passing it to the DAO.
        return "/" + StringUtils.strip(endpoint,"/");
    }
}
