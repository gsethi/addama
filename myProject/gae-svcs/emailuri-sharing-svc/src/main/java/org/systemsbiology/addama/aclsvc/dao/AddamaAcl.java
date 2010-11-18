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

/**
 * @author trobinso (adapted from the MRMAtlas project)
 *
 * Access control list object internal representation for Addama Services.
 * This specifies the internal format used to represent a single ACL entry.
 *
 * For the canonicalization and serialization of this object from JSON data, see AddamaAclSetDto.
 */
public class AddamaAcl {
    public enum AddamaAclPermission {
        // Permissions, listed in ascending order of permission
        NO_ACCESS,
        READ,
        WRITE,
        FULL_CONTROL
    }
    public enum AddamaAclScope {
        // Scope, listed in ascending order of inclusion
        UserById,
        UserByEmail,
        GroupById,
        GroupByEmail,
        //GroupByDomain,
        AllAuthenticatedUsers,
        AllUsers,
        Everyone
    }
    private AddamaAclPermission permission;
    private AddamaAclScope scope;
    private String credential;

    public AddamaAcl() {}

    public AddamaAcl(AddamaAclPermission permission, AddamaAclScope scope, String credential) {
        this.permission = permission;
        this.scope = scope;
        this.credential = credential;
    }

    public AddamaAclPermission getPermission() {
        return permission;
    }

    public void setPermission(AddamaAclPermission permission) {
        this.permission = permission;
    }

    public AddamaAclScope getScope() {
        return scope;
    }

    public void setScope(AddamaAclScope scope) {
        this.scope = scope;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }
}
