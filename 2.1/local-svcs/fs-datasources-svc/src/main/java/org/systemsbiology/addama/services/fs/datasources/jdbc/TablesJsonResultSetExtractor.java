package org.systemsbiology.addama.services.fs.datasources.jdbc;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.substringAfterLast;

/**
 * @author hrovira
 */
public class TablesJsonResultSetExtractor implements ResultSetExtractor {
    private static final Logger log = Logger.getLogger(TablesJsonResultSetExtractor.class.getName());

    public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
        JSONObject json = new JSONObject();
        while (rs.next()) {
            try {
                String uri = rs.getString("URI");
                String filename = substringAfterLast(uri, "/");

                JSONObject itemjson = new JSONObject();
                itemjson.put("uri", uri);
                itemjson.put("name", filename);
                itemjson.put("label", filename);
                itemjson.put("table", rs.getString("TABLE_NAME"));

                json.append("items", itemjson);
            } catch (JSONException e) {
                log.warning(e.getMessage());
            }
        }
        return json;
    }

}
