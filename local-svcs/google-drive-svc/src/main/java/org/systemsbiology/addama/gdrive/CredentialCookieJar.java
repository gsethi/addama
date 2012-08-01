package org.systemsbiology.addama.gdrive;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;

/**
 * @author hrovira
 */
public class CredentialCookieJar {
    public static final String COOKIE_USER_ID = "x_addama_gds_user";

    public static String getUserFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (equalsIgnoreCase(cookie.getName(), COOKIE_USER_ID)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public static void markRejected(HttpServletResponse response) {
        Cookie c = new Cookie(COOKIE_USER_ID, "REJECTED");
        c.setMaxAge(-1);
        c.setPath("/");
        response.addCookie(c);
    }

    public static void giveCookie(HttpServletResponse response, String userId) {
        Cookie c = new Cookie(COOKIE_USER_ID, userId);
        c.setMaxAge(-1);
        c.setPath("/");
        response.addCookie(c);
    }

    public static void snatchCookie(HttpServletResponse response) {
        Cookie c = new Cookie(COOKIE_USER_ID, "");
        c.setMaxAge(0);
        c.setPath("/");
        response.addCookie(c);
    }
}
