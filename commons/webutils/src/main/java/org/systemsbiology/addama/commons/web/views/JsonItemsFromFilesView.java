package org.systemsbiology.addama.commons.web.views;

import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.systemsbiology.addama.commons.web.utils.HttpIO.getMimeType;

/**
 * @author hrovira
 */
public class JsonItemsFromFilesView extends JsonItemsView {
    public static final String URI = "URI";
    public static final String FILES = "FILES";
    public static final String READ_ONLY = "READ_ONLY";

    public String getContentType() {
        return "application/json";
    }

    public void render(Map map, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = (String) map.get(URI);
        File[] files = (File[]) map.get(FILES);
        Boolean readOnly = (Boolean) map.get(READ_ONLY);

        JSONObject json = new JSONObject();
        json.put("uri", uri);
        json.put("readOnly", readOnly);
        for (File f : files) {
            json.append("items", fileAsJson(uri + "/" + f.getName(), f, request));
        }

        map.put("json", json);

        super.render(map, request, response);
    }

    private JSONObject fileAsJson(String uri, File f, HttpServletRequest request) throws JSONException, IOException {
        String filename = f.getName();

        JSONObject json = new JSONObject();
        json.put("name", filename);
        json.put("label", filename);
        json.put("uri", uri);

        boolean isFile = f.isFile();
        json.put("isFile", isFile);
        if (isFile) {
            json.put("size", f.length());
            json.put("mimeType", getMimeType(request, f));
        }
        return json;
    }
}
