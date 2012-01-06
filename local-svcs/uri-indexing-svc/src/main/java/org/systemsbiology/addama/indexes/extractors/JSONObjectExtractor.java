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
package org.systemsbiology.addama.indexes.extractors;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.json.JSONException;
import org.json.JSONObject;
import org.springmodules.lucene.search.core.HitExtractor;

import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;

/**
 * @author hrovira
 */
public class JSONObjectExtractor implements HitExtractor {
    public Object mapHit(int i, Document document, float v) {
        try {
            JSONObject json = new JSONObject();
            for (Object object : document.getFields()) {
                Field field = (Field) object;
                String fieldName = field.name();
                if (isIncluded(fieldName)) {
                    json.accumulate(fieldName, field.stringValue());
                }
            }
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isIncluded(String fieldName) {
        String[] excludedFields = new String[]{"paths"};
        for (String excludedField : excludedFields) {
            if (equalsIgnoreCase(excludedField, fieldName)) {
                return false;
            }
        }
        return true;
    }

}
