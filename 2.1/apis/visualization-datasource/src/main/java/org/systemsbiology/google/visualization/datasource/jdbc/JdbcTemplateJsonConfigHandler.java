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
package org.systemsbiology.google.visualization.datasource.jdbc;

import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.systemsbiology.addama.jsonconfig.impls.GenericMapJsonConfigHandler;

import java.util.Map;

import static org.systemsbiology.google.visualization.datasource.JdbcTemplateHelper.getJdbcTemplate;

/**
 * @author hrovira
 */
public class JdbcTemplateJsonConfigHandler extends GenericMapJsonConfigHandler<JdbcTemplate> {

    public JdbcTemplateJsonConfigHandler(Map<String, JdbcTemplate> map) {
        super(map);
    }

    public JdbcTemplate getSpecific(JSONObject local) throws Exception {
        return getJdbcTemplate(local);
    }

}
