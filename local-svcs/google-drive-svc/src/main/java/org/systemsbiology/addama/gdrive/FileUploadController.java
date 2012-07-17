package org.systemsbiology.addama.gdrive;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.oauth2.model.Userinfo;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.editors.JSONObjectPropertyEditor;
import org.systemsbiology.addama.commons.web.exceptions.FailedAuthenticationException;
import org.systemsbiology.addama.commons.web.views.JsonView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static com.google.api.client.http.ByteArrayContent.fromString;
import static java.lang.System.currentTimeMillis;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.systemsbiology.addama.gdrive.CredentialMediator.NoRefreshTokenException;

/**
 * @author hrovira
 */
@Controller
public class FileUploadController {
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(JSONObject.class, new JSONObjectPropertyEditor());
    }

    @RequestMapping(method = GET)
    protected ModelAndView start(HttpServletRequest request) throws Exception {
        JSONObject json = new JSONObject();
        try {
            CredentialMediator mediator = new CredentialMediator(request);
            Userinfo userinfo = mediator.getActiveUserinfo();
            json.put("client_id", mediator.getClientId());
            json.put("email", userinfo.getEmail());
            json.put("familyName", userinfo.getFamilyName());
            json.put("givenName", userinfo.getGivenName());
            json.put("name", userinfo.getName());
            json.put("picture", userinfo.getPicture());
        } catch (NoRefreshTokenException e) {
            json.put("redirect", e.getAuthorizationUrl());
        }
        json.put("lastChange", lastChange(request, false));
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/callback")
    protected ModelAndView callback(HttpServletRequest request, HttpServletResponse response,
                                    @RequestParam(value = "code", required = false) String code,
                                    @RequestParam(value = "error", required = false) String error) throws Exception {
        try {
            CredentialMediator mediator = new CredentialMediator(request);
            if (!isEmpty(code)) {
                mediator.storeCallbackCode(code);
                response.sendRedirect("static/close.html");
            } else {
                response.sendRedirect("static/close.html?error=" + error);
            }
            return null;
        } finally {
            lastChange(request, true);
        }
    }

    @RequestMapping(value = "/**/logout")
    protected ModelAndView logout(HttpServletRequest request) throws Exception {
        CredentialMediator mediator = new CredentialMediator(request);
        mediator.deleteCredentials();
        lastChange(request, true);

        JSONObject json = new JSONObject();
        json.put("lastChange", lastChange(request, false));
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(method = POST)
    protected ModelAndView upload(HttpServletRequest request, @RequestParam("meta") JSONObject meta,
                                  @RequestParam("content") String content) throws Exception {
        CredentialMediator mediator = new CredentialMediator(request);
        JSONObject json = new JSONObject();
        try {
            String filename = meta.getString("title");
            String mimeType = getMimeType(meta, request);

            File file = new File();
            file.setTitle(filename);
            if (meta.has("description")) {
                file.setDescription(meta.getString("description"));
            }
            file.setMimeType(mimeType);

            Drive drive = mediator.getDriveService();
            File uploaded = drive.files().insert(file, fromString(mimeType, content)).execute();

            json.put("id", uploaded.getId());
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == SC_UNAUTHORIZED) {
                mediator.deleteCredentials();
                throw new FailedAuthenticationException(mediator.getUserId());
            }
            throw e;
        } catch (NoRefreshTokenException e) {
            json.put("redirect", e.getAuthorizationUrl());
        }
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    private String getMimeType(JSONObject meta, HttpServletRequest request) throws JSONException {
        String mimeType;

        if (meta.has("mimeType")) {
            mimeType = meta.getString("mimeType");
        } else {
            String filename = meta.getString("title");
            mimeType = request.getSession().getServletContext().getMimeType(filename);
        }

        if (isEmpty(mimeType)) {
            mimeType = "text/plain";
        }
        return mimeType;
    }

    private Object lastChange(HttpServletRequest request, boolean forceUpdate) {
        HttpSession session = request.getSession();
        synchronized (session.getId()) {
            Object lastChange = session.getAttribute("LAST_CHANGE");
            if (lastChange == null || forceUpdate) {
                lastChange = currentTimeMillis();
                session.setAttribute("LAST_CHANGE", lastChange);
            }
            return lastChange;
        }
    }

}
