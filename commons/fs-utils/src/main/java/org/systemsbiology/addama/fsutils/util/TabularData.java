package org.systemsbiology.addama.fsutils.util;

import org.springframework.core.io.Resource;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.Double.parseDouble;
import static org.apache.commons.lang.StringUtils.*;

/**
 * @author hrovira
 */
public class TabularData {
    private static final Logger log = Logger.getLogger(TabularData.class.getName());

    public static String tabularColumnSplitter(String columnHeaders) {
        if (!isEmpty(columnHeaders)) {
            if (contains(columnHeaders, "\t")) {
                return "\t";
            }
            if (contains(columnHeaders, ",")) {
                return ",";
            }
        }
        return null;
    }

    public static String guessedDataType(String value) {
        if (!isEmpty(value)) {
            if (equalsIgnoreCase(value, "true") || equalsIgnoreCase(value, "false")) {
                return "boolean";
            }
            try {
                parseDouble(value);
                return "number";
            } catch (NumberFormatException e) {
                log.warning(value + ":" + e);
            }
        }
        return "string";
    }

    public static Map<String, String> asSchema(Resource resource) throws Exception {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String columnHeader = reader.readLine();
            String splitter = tabularColumnSplitter(columnHeader);
            if (isEmpty(splitter)) {
                throw new InvalidSyntaxException("columns are not separated by tab or comma");
            }

            String[] headers = columnHeader.split(splitter);
            String firstLine = reader.readLine();
            String[] values = firstLine.split(splitter);

            if (headers.length != values.length) {
                throw new InvalidSyntaxException("number of column headers do not match number of value columns");
            }

            Map<String, String> schema = new HashMap<String, String>();
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i];
                String value = values[i];
                schema.put(header, guessedDataType(value));
            }
            return schema;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                log.warning(e.getMessage());
            }
        }
    }
}
