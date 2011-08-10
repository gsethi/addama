package org.systemsbiology.addama.filedbcreate.jdbc;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class ColumnBeansJsonResultSetExtractor implements ResultSetExtractor {
    private static final Logger log = Logger.getLogger(ColumnBeansJsonResultSetExtractor.class.getName());

    public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
        JSONObject json = new JSONObject();
        while (rs.next()) {
            try {
                String label = rs.getString("COLUMN_LABEL");
                String tableUri = rs.getString("TABLE_URI");

                JSONObject item = new JSONObject();
                item.put("uri", tableUri + "/columns/" + label);
                item.put("name", rs.getString("COLUMN_NAME"));
                item.put("label", label);
                item.put("datatype", rs.getString("DATA_TYPE"));
                item.put("datasource", tableUri);

                json.append("items", item);
            } catch (JSONException e) {
                log.warning(e.getMessage());
            }
        }
        return json;
    }
}
