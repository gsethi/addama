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

import org.springframework.jdbc.core.JdbcTemplate;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.MappingsHandler;
import org.systemsbiology.addama.jsonconfig.impls.MappingPropertyByIdContainer;

import java.util.Map;

import static org.systemsbiology.google.visualization.datasource.JdbcTemplateHelper.getJdbcTemplate;

/**
 * @author hrovira
 */
public class JdbcTemplateMappingsHandler extends MappingPropertyByIdContainer<JdbcTemplate> implements MappingsHandler {

    public JdbcTemplateMappingsHandler(Map<String, JdbcTemplate> map) {
        super(map);
    }

    public void handle(Mapping mapping) throws Exception {
        addValue(mapping, getJdbcTemplate(mapping.JSON()));
    }

}
