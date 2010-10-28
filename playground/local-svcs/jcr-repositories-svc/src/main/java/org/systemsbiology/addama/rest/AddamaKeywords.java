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
package org.systemsbiology.addama.rest;

import org.apache.commons.lang.StringUtils;

/**
 * @author hrovira
 */
public enum AddamaKeywords {
    /*
     * Enumerated Elements
     */
    addama_uri("addama-uri"),
    addama_date("addama-date"),
    addama_date_pattern("date-pattern"),
    addama_date_value("date-value"),
    addama_generate_date("addama-generate-date"),
    addama_file("addama-file"),

    created_by("created-by"),
    created_at("created-at"),
    last_modified_by("last-modified-by"),
    last_modified_at("last-modified-at");

    private final String word;

    /*
     * Constructor
     */

    AddamaKeywords(String value) {
        this.word = value;
    }

    /*
     * Public Methods
     */

    public String word() {
        if (this.word == null) {
            return this.name();
        }
        return this.word;
    }

    public boolean isContainedIn(String container) {
        if (StringUtils.isEmpty(container)) return false;
        return StringUtils.contains(container, this.word());
    }

    public boolean isEqual(String value) {
        if (StringUtils.isEmpty(value)) return false;
        return StringUtils.equals(value, this.word());
    }

    public boolean isStartOf(String value) {
        if (StringUtils.isEmpty(value)) return false;
        return value.startsWith(this.word());
    }
}
