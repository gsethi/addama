package org.systemsbiology.addama.appengine.util;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;

import javax.servlet.http.HttpServletRequest;

import static com.google.appengine.api.users.UserServiceFactory.getUserService;
import static org.systemsbiology.addama.appengine.util.ApiKeys.getUserEmailFromApiKey;
import static org.systemsbiology.addama.appengine.util.ApiKeys.isAdmin;

/**
 * @author hrovira
 */
public class Users {
    private static final UserService userService = getUserService();

    public static String getLoggedInUserEmail(HttpServletRequest request) {
        if (userService.isUserLoggedIn()) {
            User user = userService.getCurrentUser();
            return user.getEmail();
        }

        String apikey = request.getHeader("x-addama-apikey");
        return getUserEmailFromApiKey(apikey);
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
