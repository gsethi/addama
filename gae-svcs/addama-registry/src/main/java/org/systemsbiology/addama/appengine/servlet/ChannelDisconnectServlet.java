package org.systemsbiology.addama.appengine.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.appengine.api.channel.ChannelServiceFactory.getChannelService;
import static org.systemsbiology.addama.appengine.util.Channels.dropChannel;

/**
 * @author hrovira
 */
public class ChannelDisconnectServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        dropChannel(getChannelService().parsePresence(request));
    }
}
