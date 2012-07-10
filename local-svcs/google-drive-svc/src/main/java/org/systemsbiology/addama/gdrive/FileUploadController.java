package org.systemsbiology.addama.gdrive;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
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
import java.io.IOException;

import static com.google.api.client.http.ByteArrayContent.fromString;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

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
    protected ModelAndView start(HttpServletRequest request) throws IOException, JSONException {
        CredentialMediator mediator = new CredentialMediator(request);
        JSONObject json = new JSONObject();
        json.put("client_id", mediator.getClientId());
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(method = POST)
    protected ModelAndView upload(HttpServletRequest request, @RequestParam("meta") JSONObject meta,
                                  @RequestParam("content") String content) throws Exception {
        CredentialMediator mediator = new CredentialMediator(request);
        try {
            String filename = meta.getString("title");
            String mimeType = request.getSession().getServletContext().getMimeType(filename);
            if (isEmpty(mimeType)) {
                mimeType = "text/plain";
            }

            File file = new File();
            file.setTitle(filename);
            if (meta.has("description")) {
                file.setDescription(meta.getString("description"));
            }
            file.setMimeType(mimeType);

            Drive drive = mediator.getDriveService();
            File uploaded = drive.files().insert(file, fromString(mimeType, content)).execute();

            JSONObject json = new JSONObject();
            json.put("id", uploaded.getId());
            return new ModelAndView(new JsonView()).addObject("json", json);
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == SC_UNAUTHORIZED) {
                mediator.deleteCredentials();
                throw new FailedAuthenticationException(mediator.getUserId());
            }
            throw e;
        }
    }

}
