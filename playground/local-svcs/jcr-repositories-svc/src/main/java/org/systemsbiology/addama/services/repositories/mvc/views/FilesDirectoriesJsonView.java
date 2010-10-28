package org.systemsbiology.addama.services.repositories.mvc.views;

import org.json.JSONArray;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.web.views.JsonView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;

/**
 * @author hrovira
 */
public class FilesDirectoriesJsonView extends JsonView {

    public void render(Map map, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject json = (JSONObject) map.get("json");
        if (json == null) json = new JSONObject();

        if (!json.has("directories")) {
            json.put("directories", new JSONArray());
        }
        if (!json.has("files")) {
            json.put("files", new JSONArray());
        }

        json.put("numberOfDirectories", json.getJSONArray("directories").length());
        json.put("numberOfFiles", json.getJSONArray("files").length());

        response.setContentType(getContentType(request));

        PrintWriter writer = response.getWriter();
        writer.print(json.toString());
    }

}
