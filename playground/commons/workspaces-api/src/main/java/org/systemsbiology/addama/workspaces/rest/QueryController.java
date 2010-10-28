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
package org.systemsbiology.addama.workspaces.rest;

import com.google.visualization.datasource.DataSourceHelper;
import com.google.visualization.datasource.DataTableGenerator;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.workspaces.callbacks.JdbcTemplateDataTableGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class QueryController extends BaseDsController {
    private static final Logger log = Logger.getLogger(QueryController.class.getName());

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String databaseUri = StringUtils.substringBeforeLast(getUri(request), "/query");

        log.info(databaseUri);

        JdbcTemplate jdbcTemplate = getJdbcTemplate(databaseUri);
        String tableName = "T_" + Math.abs(databaseUri.hashCode());
        log.info(databaseUri + ":" + tableName);

        DataTableGenerator tableGenerator = new JdbcTemplateDataTableGenerator(jdbcTemplate, tableName);
        DataSourceHelper.executeDataSourceServletFlow(request, response, tableGenerator, false);
        return null;
    }
}