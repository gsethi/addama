package org.systemsbiology.addama.chromosome.index.callbacks;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.jdbc.core.ResultSetExtractor;

/**
 * @author hrovira
 */
public interface ItemedResultSetExtractor extends ResultSetExtractor {
    public JSONObject getItems() throws JSONException;

    public String getPreparedStatement();

    public Object[] getArguments();
}
