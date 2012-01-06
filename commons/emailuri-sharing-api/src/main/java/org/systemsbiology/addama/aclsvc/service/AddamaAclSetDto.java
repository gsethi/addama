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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.systemsbiology.addama.aclsvc.service.AddamaAclDto;
import org.systemsbiology.addama.aclsvc.service.DtoException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author trobinso (adapted from the MRMAtlas project)
 *
 * Access control list data transfer object. Used to convert JSON data to a canonical representation that we can work
 * with. Each field is required, unless specified otherwise.
 *
 * The simple EBNF notation for this object follows.
 * (converted from here: http://code.google.com/apis/storage/docs/developer-guide.html#authentication
 * Terminators "{", "}", and "}," are implied by JSON notation.)
 * 
 * access-control-list::= owner entries
 *
 * owner::= owner: id
 *
 * entries::= [ entry, { entry } ]
 *
 * entry::= permission scope | scope permission
 *
 * permission::= permission: ( READ | WRITE | FULL_CONTROL )
 *
 * scope::= scope: "UserById", credential: "id"
 *      | scope: "UserByEmail", credential: "email"
 *      | scope: "GroupById", credential: "id"
 *      | scope: "GroupByEmail", credential: "email"
 *      | scope: "AllUsers"
 *      | scope: "AllAuthenticatedUsers"
 *
 * id::= id-from-registry
 *
 * email::= email-address
 *
 * text::=  { printable character excluding < and > }
 *
 * id-from-registry:  64 hex digits
 * email-address: standard RFC 822 email address
 *
 * We should expect to receive a single Access Control List package, as above. The owner will always be specified on a
 * one-off basis (that is, only one owner per ACL package), and the endpoint for the permissions will always be
 * specified separately through the request URI.
 *
 * For more information on the internal representation of this object, see AddamaAcl.
 */
public class AddamaAclSetDto implements Serializable, JSONString {
    private String owner;
    private List<AddamaAclDto> entries;

    public AddamaAclSetDto() {}

    public AddamaAclSetDto(String owner, List<AddamaAclDto> entries) {
        this.owner = owner;
        this.entries = entries;
    }
    
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<AddamaAclDto> getEntries() {
        return entries;
    }

    public void setEntries(List<AddamaAclDto> entries) {
        this.entries = entries;
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
            json.put("owner", owner);
            json.put("entries", entries);
        } catch (JSONException e) {
            String msg = "Could not convert to JSONObject: " + this.toString();
            throw new DtoException(msg, e);
        }
        return json;
    }

    public static AddamaAclSetDto fromJSONString(String jsonStr) throws DtoException {
        AddamaAclSetDto dto = new AddamaAclSetDto();

        try {
            JSONObject json = new JSONObject(jsonStr);

            dto.setOwner(json.getString("owner"));
            List<AddamaAclDto> aclDtoList = new ArrayList<AddamaAclDto>();
            JSONArray entries = json.getJSONArray("entries");
            for (Integer i = 0; i < entries.length(); ++i) {
                AddamaAclDto aclDto = AddamaAclDto.fromJSONString(entries.getString(i));
                aclDtoList.add(aclDto);
            }
            dto.setEntries(aclDtoList);
        } catch (JSONException e) {
            String msg = "Could not construct DTO from JSON: " + jsonStr;
            throw new DtoException(msg, e);
        }

        return dto;
    }

    public static AddamaAclSetDto fromAclDefinition(String owner, List<AddamaAclDto> aclDtos) throws Exception {
        AddamaAclSetDto dto = new AddamaAclSetDto();
        dto.setOwner(owner);
        dto.setEntries(aclDtos);

        return dto;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
