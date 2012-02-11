package org.systemsbiology.addama.commons.web.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import static org.apache.commons.lang.StringUtils.*;

/**
 * @author hrovira
 */
public class RegisteredUser {

    public static String getRegistryUser(HttpServletRequest request) {
        String user = request.getHeader("x-addama-registry-user");
        if (!isEmpty(user)) {
            if (contains(user, "/addama/users/")) {
                return substringAfterLast(user, "/addama/users/");
            }
            return user;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (equalsIgnoreCase("x-addama-registry-user", cookie.getName())) {
                    if (contains(user, "/addama/users/")) {
                        return substringAfterLast(cookie.getValue(), "/addama/users/");
                    }
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
