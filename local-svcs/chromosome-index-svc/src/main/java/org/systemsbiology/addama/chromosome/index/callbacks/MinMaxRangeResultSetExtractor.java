package org.systemsbiology.addama.chromosome.index.callbacks;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author hrovira
 */
public class MinMaxRangeResultSetExtractor implements ResultSetExtractor<JSONObject> {
    private final String col_start;
    private final String col_end;
    private final String tableName;
    private final String col_chrom;

    public MinMaxRangeResultSetExtractor(JSONObject schema) throws JSONException {
        tableName = schema.getString("table");
        col_chrom = schema.optString("chromosome", "chrom");
        col_start = schema.optString("start", "start");
        col_end = schema.optString("end", "end");
    }

    public String SQL() {
        return "SELECT MIN(" + col_start + ") AS CST, MAX(" + col_end + ") AS CSE FROM " + tableName + " WHERE " + col_chrom + " = ?";
    }

    public JSONObject extractData(ResultSet rs) throws SQLException, DataAccessException {
        try {
            if (rs.next()) {
                JSONObject json = new JSONObject();
                json.put("start", rs.getInt("CST"));
                json.put("end", rs.getInt("CSE"));
                return json;
            }
        } catch (JSONException e) {
            throw new DataRetrievalFailureException(e.getMessage(), e);
        }
        return null;
    }

}
