package org.systemsbiology.addama.appengine.util;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;

import javax.servlet.http.HttpServletRequest;

import static com.google.appengine.api.users.UserServiceFactory.getUserService;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.systemsbiology.addama.appengine.util.ApiKeys.getUserUriFromApiKey;
import static org.systemsbiology.addama.appengine.util.ApiKeys.isAdmin;

/**
 * @author hrovira
 */
public class Users {

    public static String getLoggedInUserUri(HttpServletRequest request) {
        UserService userService = getUserService();
        if (userService.isUserLoggedIn()) {
            User user = userService.getCurrentUser();
            return "/addama/users/" + user.getEmail();
        }

        String apikey = request.getHeader("x-addama-apikey");
        return getUserUriFromApiKey(apikey);
    }

    public static String getLoggedInUserEmail(HttpServletRequest request) {
        String userUri = getLoggedInUserUri(request);
        if (isEmpty(userUri)) {
            return null;
        }
        return substringAfterLast(userUri, "/");
    }

    public static User getCurrentUser() throws ForbiddenAccessException {
        UserService userService = getUserService();

        if (!userService.isUserLoggedIn()) {
            throw new ForbiddenAccessException();
        }

        User user = userService.getCurrentUser();
        if (user == null) {
            throw new ForbiddenAccessException();
        }

        return user;
    }

    public static boolean isAdministrator(HttpServletRequest request) {
        UserService userService = getUserService();

        if (userService.isUserLoggedIn() && userService.isUserAdmin()) {
            return true;
        }

        String apiKey = request.getHeader("x-addama-apikey");
        return isAdmin(apiKey);
    }

    public static void checkAdmin(HttpServletRequest request) throws ForbiddenAccessException {
        if (!isAdministrator(request)) {
            throw new ForbiddenAccessException(getLoggedInUserEmail(request));
        }
    }
}
