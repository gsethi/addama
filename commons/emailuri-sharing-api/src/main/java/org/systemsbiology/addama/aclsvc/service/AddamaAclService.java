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
import java.util.ArrayList;
import java.util.List;

/**
 * @author trobinso
 */
public interface AddamaAclService {
    
    /*
     * Accessors
     */

    public boolean isAllowed(String endpoint, AddamaAclDto challenge) throws Exception;

    public boolean isOwner(String endpoint, String owner) throws Exception;

    public boolean isAllRead(String endpoint) throws Exception;

    public boolean isAllWrite(String endpoint) throws Exception;

    public boolean isAllFullControl(String endpoint) throws Exception;

    public boolean isAllAuthenticatedRead(String endpoint) throws Exception;

    public boolean isAllAuthenticatedWrite(String endpoint) throws Exception;

    public boolean isAllAuthenticatedFullControl(String endpoint) throws Exception;

    public boolean isNoAccess(String endpoint) throws Exception;

    public String getOwner(String endpoint) throws Exception;

    public List<AddamaAclDto> getAcls(String endpoint) throws Exception;

    /*
    public List<AddamaAclDto> getReadable(String endpoint) throws Exception;

    public List<AddamaAclDto> getWriteable(String endpoint) throws Exception;

    public List<AddamaAclDto> getFullControl(String endpoint) throws Exception;
    */

    /*
     * Mutators
     */

    public boolean setAcl(String endpoint, AddamaAclDto target) throws Exception;

    public boolean setAcls(String endpoint, List<AddamaAclDto> targetList) throws Exception;

    public boolean setOwner(String endpoint, String owner) throws Exception;

    public boolean setAllRead(String endpoint) throws Exception;

    public boolean setAllWrite(String endpoint) throws Exception;

    public boolean setAllFullControl(String endpoint) throws Exception;

    public boolean setAllAuthenticatedRead(String endpoint) throws Exception;

    public boolean setAllAuthenticatedWrite(String endpoint) throws Exception;

    public boolean setAllAuthenticatedFullControl(String endpoint) throws Exception;

    public boolean setNoAccess(String endpoint) throws Exception;

    public boolean removeAcl(String endpoint, AddamaAclDto target) throws Exception;

    public boolean clearAcls(String endpoint) throws Exception;
}
