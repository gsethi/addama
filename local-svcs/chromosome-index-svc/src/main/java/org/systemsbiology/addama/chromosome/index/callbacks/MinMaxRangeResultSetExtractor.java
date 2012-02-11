package org.systemsbiology.addama.chromosome.index.callbacks;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.systemsbiology.addama.chromosome.index.pojos.Schema;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author hrovira
 */
public class MinMaxRangeResultSetExtractor implements ResultSetExtractor<JSONObject> {
    private final Schema schema;

    public MinMaxRangeResultSetExtractor(Schema schema) throws JSONException {
        this.schema = schema;
    }

    public String SQL() {
        return "SELECT MIN(" + schema.getStartColumn() + ") AS CST, MAX(" + schema.getEndColumn() + ") AS CSE FROM " + schema.getTableName() + " WHERE " + schema.getChromosomeColumn() + " = ?";
    }

    public JSONObject extractData(ResultSet rs) throws SQLException, DataAccessException {
        try {
            if (rs.next()) {
                JSONObject json = new JSONObject();
                int start = rs.getInt("CST");
                int end = rs.getInt("CSE");
                json.put("start", start);
                json.put("end", end);
                json.put("length", end - start);
                return json;
            }
        } catch (JSONException e) {
            throw new DataRetrievalFailureException(e.getMessage(), e);
        }
        return null;
    }

}
