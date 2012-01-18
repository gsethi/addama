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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.systemsbiology.addama.aclsvc.service.DtoException;

import java.io.Serializable;

/**
 * @author trobinso (adapted from the MRMAtlas project)
 *
 * This specifies the DTO used to represent a single ACL entry.
 *
 * For more information on the serialization workflow, see AddamaAclSetDto.
 */
public class AddamaAclDto implements Serializable, JSONString {
    private String permission;
    private String scope; 
    private String credential;

    public AddamaAclDto() {}

    public AddamaAclDto(String permission, String scope, String credential) {
        this.permission = permission;
        this.scope = scope;
        this.credential = credential;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String toJSONString() {
        String jsonStr = null;
        try {
            jsonStr = toJSONObject().toString();
        } catch (DtoException e) {
            throw new RuntimeException(e);
        }
        return jsonStr;
   }

    public JSONObject toJSONObject() throws DtoException {
        JSONObject json = new JSONObject();
        try {
            json.put("permission", permission);
            json.put("scope", scope);
            json.put("credential", credential);
        } catch (JSONException e) {
            String msg = "Could not convert to JSONObject: " + this.toString();
            throw new DtoException(msg, e);
        }
        return json;
    }

    public static AddamaAclDto fromJSONString(String jsonStr) throws DtoException {
        AddamaAclDto dto = new AddamaAclDto();

        try {
            JSONObject json = new JSONObject(jsonStr);

            dto.setPermission(json.getString("permission"));
            dto.setScope(json.getString("scope"));
            dto.setCredential(json.getString("credential"));
        } catch (JSONException e) {
            String msg = "Could not construct DTO from JSON: " + jsonStr;
            throw new DtoException(msg, e);
        }

        return dto;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}