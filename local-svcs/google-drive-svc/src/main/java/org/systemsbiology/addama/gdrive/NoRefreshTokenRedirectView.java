package org.systemsbiology.addama.gdrive;

import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author hrovira
 */
public class NoRefreshTokenRedirectView implements View {
    public String getContentType() {
        return "text/plain";
    }

    public void render(Map map, HttpServletRequest request, HttpServletResponse response) throws Exception {
        NoRefreshTokenException ex = (NoRefreshTokenException) map.get("ex");
        response.sendRedirect(ex.getAuthorizationUrl());
    }
}
