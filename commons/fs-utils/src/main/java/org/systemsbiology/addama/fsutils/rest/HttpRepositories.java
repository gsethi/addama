package org.systemsbiology.addama.fsutils.rest;

import javax.servlet.http.HttpServletRequest;

import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.cleanSpaces;

/**
 * @author hrovira
 */
public class HttpRepositories {

    /**
     * Extracts repository URI from request
     *
     * @param request   - HTTP
     * @param uriScheme - URI scheme
     * @return String - repository URI
     */
    public static String getRepositoryUri(HttpServletRequest request, UriScheme uriScheme) {
        String uri = substringAfter(request.getRequestURI(), request.getContextPath());
        if (uri.contains(uriScheme.name())) {
            uri = substringBeforeLast(uri, uriScheme.name());
        }
        return chomp(uri, "/");
    }

    /**
     * Extracts resource path from request
     *
     * @param request   - HTTP
     * @param uriScheme - URI scheme
     * @return String - repository URI
     * @todo - handle spaces
     */
    public static String getResourcePath(HttpServletRequest request, UriScheme uriScheme) {
        String requestUri = request.getRequestURI();
        if (requestUri.contains(uriScheme.name())) {
            return cleanSpaces(chomp(substringAfter(requestUri, uriScheme.name()), "/"));
        }
        return null;
    }

    /**
     * @param request   - http
     * @param uriScheme - path, file, etc...
     * @param suffix    - dir
     * @return path to resource
     */
    public static String getResourcePath(HttpServletRequest request, UriScheme uriScheme, String suffix) {
        String resourcePath = getResourcePath(request, uriScheme);
        if (resourcePath.contains(suffix)) {
            return cleanSpaces(substringBeforeLast(resourcePath, suffix));
        }
        return resourcePath;
    }
}
