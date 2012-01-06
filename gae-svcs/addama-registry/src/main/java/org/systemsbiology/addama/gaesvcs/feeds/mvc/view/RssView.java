package org.systemsbiology.addama.gaesvcs.feeds.mvc.view;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static org.systemsbiology.addama.commons.gae.Appspot.APPSPOT_ID;
import static org.systemsbiology.addama.commons.gae.Appspot.APPSPOT_URL;

/**
 * RSS 2.0 pagination API per http://tools.ietf.org/html/rfc5005#section-3
 *
 * @author hrovira
 */
public class RssView implements View {
    public static final String FEED_ID = "FEED_ID";
    public static final String PAGE_NUMBER = "PAGE_NUMBER";

    private static final String LAST_BUILD_DATE = getLastBuildDate();

    public String getContentType() {
        return "text/xml";
    }

    public void render(Map map, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject json = (JSONObject) map.get("json");
        Integer pageNum = (Integer) map.get(PAGE_NUMBER);
        String feedId = (String) map.get(FEED_ID);

        String feedUri = "/addama/feeds/" + feedId;
        String feedUrl = APPSPOT_URL() + feedUri;

        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        builder.append("<rss version=\"2.0\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\" ");
        builder.append("xmlns:atom=\"http://www.w3.org/2005/Atom\">");
        builder.append("<channel>");
        builder.append("<title>Addama Feeds</title>");
        builder.append("<link>").append(feedUrl).append("?page=").append(pageNum).append("</link>");
        builder.append("<atom:link rel=\"next\" href=\"").append(feedUrl);
        builder.append("?page=").append(pageNum + 1).append("\" />");
        builder.append("<description>Feeds for ").append(APPSPOT_ID()).append("</description>");
        builder.append("<language>en</language>");
        builder.append("<lastBuildDate>").append(LAST_BUILD_DATE).append("</lastBuildDate>");
        builder.append("<generator>").append(APPSPOT_ID()).append("</generator>");
        builder.append("<ttl>60</ttl>");
        builder.append("<image>");
        builder.append("<url>").append("/images/rss.png</url>");
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
