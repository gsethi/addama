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

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.dao.DataAccessException;
import org.systemsbiology.addama.chromosome.index.pojos.ChromUriBean;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

import static org.systemsbiology.addama.chromosome.index.pojos.ChromUriBean.Strand.unspecified;

/**
 * @author hrovira
 */
public class ChromUriResultSetExtractor extends Itemed implements ItemedResultSetExtractor {
    private static final Logger log = Logger.getLogger(ChromUriResultSetExtractor.class.getName());

    private final String tableName;
    private final String col_chromosome;
    private final String col_start;
    private final String col_end;
    private final String col_strand;
    private final String col_uri;
    private final ChromUriBean bean;

    public ChromUriResultSetExtractor(JSONObject schema, ChromUriBean bean) throws JSONException {
        this.tableName = schema.getString("table");
        this.col_chromosome = schema.optString("chromosome", "chrom");
        this.col_start = schema.optString("start", "start");
        this.col_end = schema.optString("end", "end");
        this.col_strand = schema.optString("strand", "strand");
        this.col_uri = schema.optString("uri", "uri");
        this.bean = bean;
    }

    /*
     * ItemedResultSetExtractor
     */

    public String getPreparedStatement() {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT * FROM ").append(tableName);
        builder.append(" WHERE ").append(col_chromosome).append(" = ? ");
        builder.append("   AND ").append(col_start).append(" >= ? ");
        builder.append("   AND ").append(col_end).append(" <= ? ");
        if (!unspecified.equals(bean.getStrand())) {
            builder.append("   AND ").append(col_strand).append(" = ? ");
        }
        return builder.toString();
    }

    public Object[] getArguments() {
        ArrayList<Object> args = new ArrayList<Object>();
        args.add(bean.getChromosome());
        args.add(bean.getStart());
        args.add(bean.getEnd());
        if (!unspecified.equals(bean.getStrand())) {
            args.add(bean.getStrand().getSign());
        }
        return args.toArray(new Object[args.size()]);
    }

    /*
     * ResultSetExtractor
     */

    public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
        while (rs.next()) {
            String uri = rs.getString(col_uri);
            try {
                JSONObject itemjson = new JSONObject();
                itemjson.put("uri", uri);
                itemjson.put("chromosome", rs.getString(col_chromosome));
                itemjson.put("start", rs.getInt(col_start));
                itemjson.put("end", rs.getInt(col_end));
                itemjson.put("strand", rs.getString(col_strand));
                itemjson.put("label", StringUtils.substringAfterLast(uri, "/"));

                addItem(uri, itemjson);
            } catch (JSONException e) {
                log.warning("extractData(" + uri + "):" + e);
            }
        }
        return null;
    }
}
