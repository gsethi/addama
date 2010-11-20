package org.systemsbiology.addama.gaesvcs.feeds.mvc.view;

import com.google.apphosting.api.ApiProxy;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.servlet.View;
import org.systemsbiology.addama.gaesvcs.feeds.mvc.FeedsController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * RSS 2.0 pagination API per http://tools.ietf.org/html/rfc5005#section-3
 *  
 * @author hrovira
 */
public class RssView implements View {
    private static final String APPSPOT_HOST = ApiProxy.getCurrentEnvironment().getAppId() + ".appspot.com";
    private static final String LAST_BUILD_DATE = getLastBuildDate();
    private final String feed;
    private final int pageNum;
    
    public RssView(String feed, int pageNum) {
    	this.feed = feed;
    	this.pageNum = pageNum;
    }
    
    public String getContentType() {
        return "text/xml";
    }

    public void render(Map map, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String feedUri = request.getRequestURI();
        JSONObject json = (JSONObject) map.get("json");

        String baseUrl = "https://" + APPSPOT_HOST;

        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        builder.append("<rss version=\"2.0\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\" ");
        builder.append("xmlns:atom=\"http://www.w3.org/2005/Atom\">");
        builder.append("<channel>");
        builder.append("<title>Addama Feeds</title>");
        builder.append("<link>").append(baseUrl).append(feed).append("?").append(FeedsController.PAGE_PARAM).append("=").append(pageNum).append("</link>");
        builder.append("<atom:link rel=\"next\" href=\"").append(baseUrl).append(feed).append("?").append(FeedsController.PAGE_PARAM).append("=").append(pageNum+1).append("\" />");
        builder.append("<description>Feeds for ").append(APPSPOT_HOST).append("</description>");
        builder.append("<language>en</language>");
        builder.append("<lastBuildDate>").append(LAST_BUILD_DATE).append("</lastBuildDate>");
        builder.append("<generator>").append(APPSPOT_HOST).append("</generator>");
        builder.append("<ttl>60</ttl>");
        builder.append("<image>");
        builder.append("<url>").append("/addama/services/feeds-api-svc/images/rss.png</url>");
        builder.append("<title>Addama Feeds</title>");
        builder.append("<link>").append("/addama/feeds</link>");
        builder.append("</image>");
        if (json.has("items")) {
            JSONArray items = json.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);

                // Handle optional properties
                String link = item.has("link") ? item.getString("link") : feedUri;
                String title = item.has("title") ? item.getString("title") : item.getString("text");
                builder.append("<item>");
                builder.append("<title>").append(title).append("</title>");
                builder.append("<link>").append(link).append("</link>");
                builder.append("<pubDate>").append(item.getString("date")).append("</pubDate>");
                builder.append("<description><![CDATA[").append(item.getString("text")).append("]]></description>");
                builder.append("<content></content>");
                builder.append("<author>").append(item.getString("author")).append("</author>");
//                builder.append("<guid isPermaLink=\"true\">").append(baseUrl).append(feedUri).append("</guid>");
                builder.append("</item>");
            }
        }

        builder.append("</channel>");
        builder.append("</rss>");

        response.setContentType(getContentType());
        response.getWriter().write(builder.toString());
    }

    private static String getLastBuildDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        return sdf.format(new Date());
    }
}
