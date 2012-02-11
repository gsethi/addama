package org.systemsbiology.addama.services.proxy.transforms;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author hrovira
 */
public class TsvToJsonItemsResponseTransform implements ResponseTransform {

    public void handle(InputStream inputStream, HttpServletResponse response) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        JSONObject json = new JSONObject();

        String line = reader.readLine();
        String[] headers = line.split("\t");

        while (line != null) {
            line = reader.readLine();
            if (line != null) {
                String[] row = line.split("\t");
                JSONObject item = new JSONObject();
                for (int i = 0; i < row.length; i++) {
                    item.put(headers[i], row[i]);
                }
                json.append("items", item);
            }
        }

        if (!json.has("items")) {
            json.put("items", new JSONArray());
        }
        json.put("numberOfItems", json.getJSONArray("items").length());

        response.setContentType("application/json");
        response.getWriter().write(json.toString());
    }
}
