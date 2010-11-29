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

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.net.URL;
import java.util.UUID;

/**
 * @author hrovira
 */
public class RegistryService implements Serializable {
    private String uri;
    private String sharingUri;
    private URL url;
    private String label;
    private UUID accessKey;
    private boolean searchable;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getSharingUri() {
        return sharingUri;
    }

    public void setSharingUri(String sharingUri) {
        this.sharingUri = sharingUri;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public UUID getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(UUID accessKey) {
        this.accessKey = accessKey;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append(uri).append(",");
        builder.append(url).append(",");
        builder.append(label).append(",");
        builder.append(searchable).append(",");
        if (StringUtils.isEmpty(sharingUri)) {
            builder.append("sharing=").append(sharingUri).append(",");
        }
        builder.append("accesskey=").append(accessKey != null);
        builder.append("]");
        return builder.toString();
    }
}
