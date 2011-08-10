package org.systemsbiology.addama.commons.web.views;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;

import static org.systemsbiology.addama.commons.web.utils.HttpIO.getDesiredContentType;

/**
 * @author hrovira
 */
public class FilesDirectoriesJsonView implements View {
    public String getContentType() {
        return "application/json";
    }

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

        response.setContentType(getDesiredContentType(request, this.getContentType()));

        PrintWriter writer = response.getWriter();
        writer.print(json.toString());
    }

}
