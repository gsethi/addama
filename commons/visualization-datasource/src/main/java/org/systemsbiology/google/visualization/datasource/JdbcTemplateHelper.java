package org.systemsbiology.google.visualization.datasource;

import org.apache.commons.dbcp.BasicDataSource;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author hrovira
 */
public class JdbcTemplateHelper {
    public static JdbcTemplate getJdbcTemplate(JSONObject json) throws JSONException {
        BasicDataSource bds = new BasicDataSource();
        if (json.has("classname")) bds.setDriverClassName(json.getString("classname"));
        if (json.has("jdbcurl")) bds.setUrl(json.getString("jdbcurl"));
        if (json.has("username")) bds.setUsername(json.getString("username"));
        if (json.has("password")) bds.setPassword(json.getString("password"));
        bds.setDefaultAutoCommit(getBoolean(json, "defaultAutoCommit", false));

        Integer maxRows = getInteger(json, "maxRows");
        Integer maxIdle = getInteger(json, "maxIdle");
        Integer maxActive = getInteger(json, "maxActive");
        String validationQuery = getString(json, "validationQuery");

        if (maxIdle != null) bds.setMaxIdle(maxIdle);
        if (maxActive != null) bds.setMaxActive(maxActive);
        if (!isEmpty(validationQuery)) bds.setValidationQuery(validationQuery);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(bds);
        if (maxRows != null) jdbcTemplate.setMaxRows(maxRows);

        jdbcTemplate.afterPropertiesSet();
        return jdbcTemplate;
    }

    /*
     * Private Methods
     */

    private static Integer getInteger(JSONObject json, String propName) throws JSONException {
        if (json.has(propName)) {
            return json.getInt(propName);
        }
        if (json.has(propName.toLowerCase())) {
            return json.getInt(propName.toLowerCase());
        }
        return null;
    }

    private static String getString(JSONObject json, String propName) throws JSONException {
        if (json.has(propName)) {
            return json.getString(propName);
        }
        if (json.has(propName.toLowerCase())) {
            return json.getString(propName.toLowerCase());
        }
        return null;
    }

    private static Boolean getBoolean(JSONObject json, String propName, boolean defaultValue) throws JSONException {
        if (json.has(propName)) {
            return json.getBoolean(propName);
        }
        if (json.has(propName.toLowerCase())) {
            return json.getBoolean(propName.toLowerCase());
        }
        return defaultValue;
    }
}
