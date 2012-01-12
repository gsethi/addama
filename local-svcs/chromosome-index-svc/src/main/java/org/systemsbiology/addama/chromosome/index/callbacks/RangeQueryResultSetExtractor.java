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
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.systemsbiology.addama.chromosome.index.pojos.QueryParams;
import org.systemsbiology.addama.chromosome.index.pojos.Schema;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static java.sql.Types.*;
import static org.systemsbiology.addama.chromosome.index.pojos.Strand.unspecified;

/**
 * @author hrovira
 */
public class RangeQueryResultSetExtractor implements ResultSetExtractor<Iterable<JSONObject>> {
    private final Schema schema;
    private final QueryParams queryParams;

    public RangeQueryResultSetExtractor(Schema schema, QueryParams qp) throws JSONException {
        this.schema = schema;
        this.queryParams = qp;
    }

    public String PS() {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT * FROM ").append(schema.getTableName());
        builder.append(" WHERE ").append(schema.getChromosomeColumn()).append(" = ? ");
        builder.append("   AND ").append(schema.getStartColumn()).append(" >= ? ");
        builder.append("   AND ").append(schema.getEndColumn()).append(" <= ? ");
        if (!unspecified.equals(queryParams.getStrand())) {
            builder.append("   AND ").append(schema.getStrandColumn()).append(" = ? ");
        }
        return builder.toString();
    }

    public Object[] ARGS() {
        ArrayList<Object> args = new ArrayList<Object>();
        args.add(queryParams.getChromosome());
        args.add(queryParams.getStart());
        args.add(queryParams.getEnd());
        if (!unspecified.equals(queryParams.getStrand())) {
            args.add(queryParams.getStrand().getSign());
        }
        return args.toArray(new Object[args.size()]);
    }

    /*
     * ResultSetExtractor
     */

    public Iterable<JSONObject> extractData(ResultSet rs) throws SQLException, DataAccessException {
        ArrayList<JSONObject> array = new ArrayList<JSONObject>();
        HashSet<String> preset = new HashSet<String>();
        preset.add(schema.getChromosomeColumn());
        preset.add(schema.getStartColumn());
        preset.add(schema.getEndColumn());
        preset.add(schema.getStrandColumn());

        HashMap<String, Integer> notpreset = new HashMap<String, Integer>();

        ResultSetMetaData rsmd = rs.getMetaData();
        for (int i = 0; i < rsmd.getColumnCount(); i++) {
            String columnName = rsmd.getColumnName(i);
            if (!preset.contains(columnName)) {
                notpreset.put(columnName, rsmd.getColumnType(i));
            }
        }

        while (rs.next()) {
            try {
                JSONObject json = new JSONObject();
                json.put("chromosome", rs.getString(schema.getChromosomeColumn()));
                json.put("start", rs.getInt(schema.getStartColumn()));
                json.put("end", rs.getInt(schema.getEndColumn()));
                json.put("strand", rs.getString(schema.getStrandColumn()));

                for (Map.Entry<String, Integer> entry : notpreset.entrySet()) {
                    addValue(json, rs, entry.getKey(), entry.getValue());
                }

                array.add(json);
            } catch (JSONException e) {
                throw new DataRetrievalFailureException(e.getMessage(), e);
            }
        }
        return array;
    }

    private void addValue(JSONObject json, ResultSet rs, String columnName, Integer dataType) throws SQLException, JSONException {
        switch (dataType) {
            case TINYINT:
            case SMALLINT:
            case INTEGER:
            case BIGINT:
                json.put(columnName, rs.getInt(columnName));
                break;
            case FLOAT:
            case REAL:
            case DOUBLE:
            case NUMERIC:
            case DECIMAL:
                json.put(columnName, rs.getDouble(columnName));
                break;
            case BOOLEAN:
                json.put(columnName, rs.getBoolean(columnName));
                break;
            case CHAR:
            case VARCHAR:
            case LONGVARCHAR:
            case NCHAR:
            case NVARCHAR:
            case LONGNVARCHAR:
                json.put(columnName, rs.getString(columnName));
                break;
        }
    }
}
