package org.systemsbiology.addama.appengine.servlet;

import com.google.appengine.repackaged.org.json.JSONException;
import com.google.appengine.repackaged.org.json.JSONObject;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.appengine.entities.JsonEntityStore.*;

/**
 * @author hrovira
 */
public class JsonStoreServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            JSONObject json = getJson(req.getRequestURI());
            if (json != null) {
                resp.getWriter().write(json.toString());
                return;
            }

            resp.setStatus(SC_NOT_FOUND);
        } catch (JSONException e) {
            resp.sendError(SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String objectUri = req.getRequestURI();
            if (objectUri.endsWith("/delete")) {
                rest_delete(substringBeforeLast(objectUri, "/delete"), req, resp);
                return;
            }

            String objectDomain = getObjectDomain(objectUri);
            if (isEmpty(objectDomain)) {
                resp.setStatus(SC_NOT_FOUND);
                return;
            }

            if (equalsIgnoreCase(objectDomain, objectUri)) {
                rest_create(objectDomain, req, resp);

            } else if (objectUri.startsWith(objectDomain)) {
                rest_update(objectUri, req, resp);

            }
        } catch (ResourceNotFoundException e) {
            resp.setStatus(SC_NOT_FOUND);

        } catch (InvalidSyntaxException e) {
            resp.sendError(SC_BAD_REQUEST, e.getMessage());

        } catch (JSONException e) {
            resp.sendError(SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        rest_delete(req.getRequestURI(), req, resp);
    }

    private void rest_create(String objectDomain, HttpServletRequest req, HttpServletResponse resp)
            throws JSONException, IOException, InvalidSyntaxException {
        UUID uuid = create(objectDomain, new JSONObject(req.getParameter("json")));

        JSONObject json = new JSONObject();
        json.put("uri", objectDomain + "/" + uuid.toString());
        resp.getWriter().write(json.toString());
    }

    private void rest_update(String objectUri, HttpServletRequest req, HttpServletResponse resp)
            throws JSONException, ResourceNotFoundException, InvalidSyntaxException {
        update(objectUri, new JSONObject(req.getParameter("json")));
    }

    private void rest_delete(String objectUri, HttpServletRequest req, HttpServletResponse resp) {
        try {
            delete(objectUri);

        } catch (ResourceNotFoundException e) {
            resp.setStatus(SC_NOT_FOUND);
        }
    }
}
