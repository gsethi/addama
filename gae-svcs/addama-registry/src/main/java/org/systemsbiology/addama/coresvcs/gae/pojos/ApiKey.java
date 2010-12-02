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
package org.systemsbiology.addama.coresvcs.gae.pojos;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * @author hrovira
 */
public class ApiKey {
    private final String userUri;
    private final UUID key;
    private final boolean admin;

    public ApiKey(String userUri, UUID key, boolean admin) {
        this.userUri = userUri;
        this.key = key;
        this.admin = admin;
    }

    public String getUserUri() {
        return userUri;
    }

    public UUID getKey() {
        return key;
    }

    public boolean isAdmin() {
        return admin;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("apikey", key.toString());
        json.put("user", userUri);
        json.put("isAdmin", admin);
        return json;
    }
}
