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
package org.systemsbiology.addama.aclsvc.dao;
import org.systemsbiology.addama.aclsvc.dao.AddamaAcl.AddamaAclPermission;
import org.systemsbiology.addama.aclsvc.dao.AddamaAcl.AddamaAclScope;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

// @todo Replace these with the correct DAO mapping
import org.systemsbiology.addama.aclsvc.service.AddamaAclDto;
import org.systemsbiology.addama.aclsvc.service.AddamaAclDtoMapper;
import org.systemsbiology.addama.aclsvc.service.AddamaAclSetDto;

/**
 * @author trobinso
 */
public class AddamaAclDaoImpl implements AddamaAclDao {
    //! @todo Replace me with a Google App Engine DAO implementation, instead of persistence in memory
    //! @todo Replace all of the methods below with the correct DAO mapping, as well
    private static Map<String, AddamaAclSetDto> aclStore = new HashMap<String, AddamaAclSetDto>();

    /*
     * Accessors
     */
    public String getOwner(String endpoint) throws Exception {
        if (!aclStore.containsKey(endpoint)) {
            return null;
        }
        AddamaAclSetDto aclSet = aclStore.get(endpoint);
        return aclSet.getOwner();
    }

    public List<AddamaAcl> getAcls(String endpoint) throws Exception {
        if (!aclStore.containsKey(endpoint)) {
            return null;
        }
        AddamaAclSetDto aclSet = aclStore.get(endpoint);
        List<AddamaAcl> acls = new ArrayList<AddamaAcl>();
        for (AddamaAclDto dto: aclSet.getEntries()) {
            acls.add(AddamaAclDtoMapper.addamaAclFromDto(dto));
        }
        return acls;
    }

    public AddamaAclPermission getPermission(String endpoint, AddamaAclScope scope, String credential)
            throws Exception {

        // ACL precedence is: FULL_CONTROL > WRITE > READ, where the former implies the latter permission.
        // NO_ACCESS is a reserved special case, implying that this credential is denied any access whatsoever.
        //
        // The least privilege of maximum specificity takes precedence.
        //
        // By "maximum specificity", we search for the longest available endpoint Uri first.
        //  We then define the specificity of each allowed scope as follows:
        //  (UserById || UserByEmail) > (GroupById || GroupByEmail) > AllAuthenticatedUsers > AllUsers,
        //  where "Everyone" is a wildcard implying all permissions, and is nonsensical for a permission check.
        //
        // By "least privilege", we mean that in the event of duplicate permissions at the same level of specificity,
        //  only the minimum privilege will be used. Duplicate permissions will always resolve as the lowest permission
        //  for that level of specificity.

        if (endpoint == null || scope == null || credential == null) {
            throw new Exception("getPermission:: Invalid parameters: "
            + (endpoint == null ? "(endpoint was null)" : "")
            + (scope == null ? "(scope was null)" : "")
            + (credential == null ? "(credential was null)" : ""));
        }

        // "Everyone" is nonsensical as a permission query, and should always return "NO_ACCESS".
        if (scope == AddamaAclScope.Everyone) {
            return AddamaAclPermission.NO_ACCESS;
        }

        //! (Security note: this may disclose information about the permissions being checked, given the predictable
        //!  delay as the endpoint is slowly shifted downwards and ACLs are verified. Caching may help, but can be
        //!  trivialized by trying different invalid credentials. Thus, the control flow is worth exploring here.)
        while (!endpoint.equals("")) {

            // Whittle down until we find the longest available endpoint
            while (!aclStore.containsKey(endpoint)) {
                if (endpoint.equals("")) {
                    return AddamaAclPermission.NO_ACCESS;
                }
                endpoint = StringUtils.substringBeforeLast(endpoint, "/");
            }

            // Retrieve the ACL set
            AddamaAclSetDto aclSet = aclStore.get(endpoint);

            // Set up the permission map
            Map<AddamaAclScope,AddamaAclPermission> permMap = new HashMap<AddamaAclScope,AddamaAclPermission>();
            for (AddamaAclDto dto: aclSet.getEntries()) {

                AddamaAcl acl = AddamaAclDtoMapper.addamaAclFromDto(dto);
                AddamaAclScope innerScope = acl.getScope();
                String innerCredential = acl.getCredential();

                // Generate a map of least privilege at each level of specificity
                //  (thus, only use the lowest ACL for each specific level)

                // @todo Retrieve a user's group and other credentials here, then map accordingly
                permMap.put(innerScope, permMap.containsKey(innerScope)
                                            ? leastPrivilege(permMap.get(innerScope),acl.getPermission())
                                            : acl.getPermission());
            }
            // We should thus output a map containing the lowest permission at each unique level of specificity.
            // We'll need to merge equivalent levels (ie, Email and Id for a User) in the step below.

            // Thus, we walk each permission until we find the minimum permission of highest specificity
            //  (We *could* use of the properties of our AddamaAclScope enum here.
            //  For paranoia, we instead specify the order of precedence explicitly.)

            if (permMap.containsKey(AddamaAclScope.UserById)) {
                if (permMap.containsKey(AddamaAclScope.UserByEmail)) {
                    return leastPrivilege(permMap.get(AddamaAclScope.UserById), permMap.get(AddamaAclScope.UserByEmail));
                }
                return permMap.get(AddamaAclScope.UserById);
            }
            else if (permMap.containsKey(AddamaAclScope.UserByEmail)) {
                // Implied above:
                //if (permMap.containsKey(AddamaAclScope.UserById)) { return leastPermission(...); }
                return permMap.get(AddamaAclScope.UserById);
            }
            else if (permMap.containsKey(AddamaAclScope.GroupById)) {
                if (permMap.containsKey(AddamaAclScope.GroupByEmail)) {
                    return leastPrivilege(permMap.get(AddamaAclScope.GroupById), permMap.get(AddamaAclScope.GroupByEmail));
                }
                return permMap.get(AddamaAclScope.GroupById);
            }
            else if (permMap.containsKey(AddamaAclScope.GroupByEmail)) {
                // Implied above:
                //if (permMap.containsKey(AddamaAclScope.GroupById)) { return leastPermission(...); }
                return permMap.get(AddamaAclScope.GroupByEmail);
            }
            else if (permMap.containsKey(AddamaAclScope.AllAuthenticatedUsers)) {
                return permMap.get(AddamaAclScope.AllAuthenticatedUsers);
            }
            else if (permMap.containsKey(AddamaAclScope.AllUsers)) {
                return permMap.get(AddamaAclScope.AllUsers);
            }

            // Chomp down more of the string
            endpoint = StringUtils.substringBeforeLast(endpoint, "/");
        }
        return AddamaAclPermission.NO_ACCESS;
    }

    public boolean hasAccess(String endpoint) throws Exception {
        // Return whether this endpoint has permissions that aren't NO_ACCESS
        while (!endpoint.equals("")) {
            while (!aclStore.containsKey(endpoint)) {
                endpoint = StringUtils.substringBeforeLast(endpoint, "/");
                if (endpoint.equals("")) {
                    return false;
                }
            }
            AddamaAclSetDto aclSet = aclStore.get(endpoint);
            for (AddamaAclDto dto: aclSet.getEntries()) {
                AddamaAcl acl = AddamaAclDtoMapper.addamaAclFromDto(dto);
                if (acl.getPermission() != AddamaAclPermission.NO_ACCESS) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * Mutators
     */
    public boolean setOwner(String endpoint, String owner) throws Exception {
        if (!aclStore.containsKey(endpoint)) {
            return false;
        }
        AddamaAclSetDto aclSet = aclStore.get(endpoint);
        aclSet.setOwner(owner);
        return true;
    }

    public boolean clearAcls(String endpoint) throws Exception {
        if (!aclStore.containsKey(endpoint)) {
            return false;
        }
        aclStore.remove(endpoint);    
        return true;
    }

    public boolean updateAcl(String endpoint, AddamaAcl acl) throws Exception {
        // Update a specific ACL
        AddamaAclDto dto = AddamaAclDtoMapper.dtoFromAddamaAcl(acl);
        if (!aclStore.containsKey(endpoint)) {
            // Create a new endpoint for this ACL
            aclStore.put(endpoint, new AddamaAclSetDto(dto.getCredential(), new ArrayList<AddamaAclDto>()));
        }
        AddamaAclSetDto aclSet = aclStore.get(endpoint);
        for (AddamaAclDto innerDto: aclSet.getEntries()) {
            AddamaAcl oldAcl = AddamaAclDtoMapper.addamaAclFromDto(innerDto);
            if (oldAcl.getScope() == acl.getScope() &&
                oldAcl.getCredential().equals(acl.getCredential())) {
                dto.setPermission(acl.getPermission().toString());
                return true;
            }
        }
        aclSet.getEntries().add(dto);
        return true;
    }

    public boolean replaceAcls(String endpoint, List<AddamaAcl> acls) throws Exception {
        // Replace all of the ACLs in a given set
        if (!aclStore.containsKey(endpoint)) {
            return false;
        }
        AddamaAclSetDto aclSet = aclStore.get(endpoint);
        List<AddamaAclDto> aclDtos = new ArrayList<AddamaAclDto>();
        for (AddamaAcl acl: acls) {
            aclDtos.add(AddamaAclDtoMapper.dtoFromAddamaAcl(acl));
        }
        aclSet.setEntries(aclDtos);
        return true;
    }

    public boolean deleteAcl(String endpoint, AddamaAcl acl) throws Exception {
        // Delete a specific ACL
        if (!aclStore.containsKey(endpoint)) {
            return false;
        }
        AddamaAclSetDto aclSet = aclStore.get(endpoint);
        List<AddamaAclDto> dtoList = aclSet.getEntries();
        for (AddamaAclDto dto: dtoList) {
            AddamaAcl oldAcl = AddamaAclDtoMapper.addamaAclFromDto(dto);
            if (oldAcl.getScope() == acl.getScope() &&
                oldAcl.getCredential().equals(acl.getCredential())) {
                dtoList.remove(dto);
                break;
            }
        }
        return true;
    }

    /*
     * Private (helper) methods
     */
    private AddamaAclPermission leastPrivilege(AddamaAclPermission left, AddamaAclPermission right) {
        //! (Security note: This solution is valid, but relies on the structuring of our enum from least to most
        //!  permission. To improve this, a more paranoid comparator may be advised.)
        return left.compareTo(right) < 0 ? left : right;
    }
}
