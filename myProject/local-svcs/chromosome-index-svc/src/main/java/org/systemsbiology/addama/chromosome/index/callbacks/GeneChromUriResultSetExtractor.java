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
package org.systemsbiology.addama.chromosome.index.callbacks;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.systemsbiology.addama.chromosome.index.pojos.ChromUriBean;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class GeneChromUriResultSetExtractor implements ResultSetExtractor {
    private static final Logger log = Logger.getLogger(GeneChromUriResultSetExtractor.class.getName());

    private final String tableName;
    private final String col_geneuri;
    private final String col_chromosomeuri;
    private final String geneUri;

    public GeneChromUriResultSetExtractor(JSONObject schema, String geneUri) throws JSONException {
        this.tableName = schema.getString("table");
        this.col_geneuri = schema.optString("geneUri", "geneUri");
        this.col_chromosomeuri = schema.optString("chromUri", "chromUri");
        this.geneUri = geneUri;
    }

    public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
        ArrayList<ChromUriBean> beans = new ArrayList<ChromUriBean>();
        while (rs.next()) {
            String chromUri = rs.getString(this.col_chromosomeuri);
            log.info("chromUri=" + chromUri);
            beans.add(new ChromUriBean(chromUri));
        }
        return beans.toArray(new ChromUriBean[beans.size()]);
    }

    public String getPreparedStatement() {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT ").append(col_chromosomeuri);
        builder.append(" FROM ").append(tableName);
        builder.append(" WHERE ").append(col_geneuri).append(" = ? ");
        return builder.toString();
    }

    public Object[] getArguments() {
        return new Object[]{geneUri};
    }
}
