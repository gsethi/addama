package org.systemsbiology.addama.fsutils.controllers.workspaces;

import org.json.JSONObject;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.fsutils.controllers.FileSystemController;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.Double.parseDouble;
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.getCleanUri;

/**
 * @author hrovira
 */
@Controller
public class SchemaController extends FileSystemController {
    private static final Logger log = Logger.getLogger(SchemaController.class.getName());

    @RequestMapping(value = "/**/schema", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView schema(HttpServletRequest request) throws Exception {
        String uri = getCleanUri(request, "/schema");

        Resource resource = getWorkspaceResource(uri);
        if (!resource.exists()) {
            throw new ResourceNotFoundException(uri);
        }

        Map<String, String> schema = getSchema(resource);

        JSONObject json = new JSONObject();
        for (Map.Entry<String, String> entry : schema.entrySet()) {
            JSONObject column = new JSONObject();
            column.put("name", entry.getKey());
            column.put("datatype", entry.getValue());
            json.append("items", column);
        }
        json.put("comment", "data types presented here are best guesses");

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    /*
     * Private Methods
     */

    private Map<String, String> getSchema(Resource resource) throws Exception {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String columnHeader = reader.readLine();
            String splitter = getSplitter(columnHeader);

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

    private String guessedDataType(String value) {
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

    private String getSplitter(String columnHeader) throws InvalidSyntaxException {
        if (!isEmpty(columnHeader)) {
            if (contains(columnHeader, "\t")) {
                return "\t";
            }
            if (contains(columnHeader, ",")) {
                return ",";
            }
        }
        throw new InvalidSyntaxException("file does not seem to be tabular");
    }

}
