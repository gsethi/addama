package org.systemsbiology.addama.services.proxy.transforms;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * @author hrovira
 */
public class TsvToJsonArrayResponseTransform implements ResponseTransform {

    public void handle(InputStream inputStream, HttpServletResponse response) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        JSONArray jsonArray = new JSONArray();

        String line = reader.readLine();
        String[] headers = line.split("\t");

        while (line != null) {
            line = reader.readLine();
            if (line != null) {
                String[] row = line.split("\t");
                JSONObject json = new JSONObject();
                for (int i = 0; i < row.length; i++) {
                    json.put(headers[i], row[i]);
                }
                jsonArray.put(json);
            }
        }

        response.setContentType("application/json");
        response.getWriter().write(jsonArray.toString());
    }
}
