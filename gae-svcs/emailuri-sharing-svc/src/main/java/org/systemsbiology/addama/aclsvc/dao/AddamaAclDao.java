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
import java.util.List;

/**
 * @author trobinso (adapted from code by hrovira)
 */
public interface AddamaAclDao {

    /*
     * Accessors
     */
    public String getOwner(String endpoint) throws Exception;

    public List<AddamaAcl> getAcls(String endpoint) throws Exception;

    public AddamaAclPermission getPermission(String endpoint, AddamaAclScope scope, String credential) throws Exception;

    public boolean hasAccess(String endpoint) throws Exception;

    /*
     * Mutators
     */

    public boolean setOwner(String endpoint, String owner) throws Exception;

    public boolean clearAcls(String endpoint) throws Exception;

    public boolean updateAcl(String endpoint, AddamaAcl acl) throws Exception;

    public boolean replaceAcls(String endpoint, List<AddamaAcl> acls) throws Exception;

    public boolean deleteAcl(String endpoint, AddamaAcl acl) throws Exception;
    
}
