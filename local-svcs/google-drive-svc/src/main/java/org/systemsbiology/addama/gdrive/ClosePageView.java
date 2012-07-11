package org.systemsbiology.addama.gdrive;

import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author hrovira
 */
public class ClosePageView implements View {
    public String getContentType() {
        return "text/html";
    }

    @Override
    public void render(Map m, HttpServletRequest request, HttpServletResponse response) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("<body>");
        builder.append("<center>");
        builder.append("<h3>Authorization Processed</h3>");
        builder.append("<br/><a href='#' onclick='javascript:window.close(); return false;'>Close this window</a>");
        builder.append("</center>");
        builder.append("</body>");
        builder.append("</html>");
        response.setContentType("text/html");
        response.getWriter().print(builder.toString());
    }
}





