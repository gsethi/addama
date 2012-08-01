package org.systemsbiology.addama.gdrive;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Insert;
import com.google.api.services.drive.model.File;
import com.google.api.services.oauth2.model.Userinfo;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
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
import org.systemsbiology.addama.commons.web.views.OkResponseView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.google.api.client.http.ByteArrayContent.fromString;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.systemsbiology.addama.gdrive.CredentialCookieJar.*;
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
    protected ModelAndView start(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        CredentialMediator mediator = new CredentialMediator(request);
        try {
            Userinfo userinfo = mediator.getActiveUserinfo();
            json.put("client_id", mediator.getClientId());
            json.put("email", userinfo.getEmail());
            json.put("familyName", userinfo.getFamilyName());
            json.put("givenName", userinfo.getGivenName());
            json.put("name", userinfo.getName());
            json.put("picture", userinfo.getPicture());
        } catch (NoRefreshTokenException e) {
            json.put("redirect", e.getAuthorizationUrl());
            if (mediator.wasRejected()) {
                snatchCookie(response);
                json.put("rejected", true);
            }
        }
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/callback")
    protected ModelAndView callback(HttpServletRequest request, HttpServletResponse response,
                                    @RequestParam(value = "code", required = false) String code,
                                    @RequestParam(value = "error", required = false) String error) throws Exception {
        CredentialMediator mediator = new CredentialMediator(request);
        if (!isEmpty(code)) {
            String userId = mediator.storeCallbackCode(code);
            giveCookie(response, userId);
            response.sendRedirect("static/close.html");
        } else {
            markRejected(response);
            response.sendRedirect("static/close.html?error=" + error);
        }
        return null;
    }

    @RequestMapping(value = "/**/logout")
    protected ModelAndView logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        CredentialMediator mediator = new CredentialMediator(request);
        mediator.deleteCredentials();
        snatchCookie(response);
        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(method = POST)
    protected ModelAndView upload(HttpServletRequest request, HttpServletResponse response,
                                  @RequestParam("meta") JSONObject meta,
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
            Insert insert = getFileInsert(drive, file, meta, content);
            if (meta.has("asGoogleDoc") && meta.getBoolean("asGoogleDoc")) {
                insert.setConvert(true);
            }

            File uploaded = insert.execute();
            json.put("id", uploaded.getId());
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == SC_UNAUTHORIZED) {
                mediator.deleteCredentials();
                snatchCookie(response);
                throw new FailedAuthenticationException(getUserFromCookie(request));
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

    private Insert getFileInsert(Drive drive, File file, JSONObject meta, String content) throws Exception {
        if (meta.has("svgToPng") && meta.getBoolean("svgToPng")) {
            return drive.files().insert(file, svgToPng(content));
        }
        return drive.files().insert(file, fromString(file.getMimeType(), content));
    }

    private ByteArrayContent svgToPng(String svgContent) throws TranscoderException, IOException {
        PNGTranscoder t = new PNGTranscoder();

        ByteArrayOutputStream ostream = new ByteArrayOutputStream();

        TranscoderInput input = new TranscoderInput(new ByteArrayInputStream(svgContent.getBytes()));
        TranscoderOutput output = new TranscoderOutput(ostream);
        t.transcode(input, output);

        ostream.flush();

        return new ByteArrayContent("image/png", ostream.toByteArray());
    }
}
