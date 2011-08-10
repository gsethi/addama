package org.systemsbiology.addama.fsutils.controllers.repositories;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.systemsbiology.addama.fsutils.controllers.FileSystemController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang.StringUtils.contains;
import static org.systemsbiology.addama.fsutils.rest.HttpRepositories.getRepositoryUri;
import static org.systemsbiology.addama.fsutils.rest.HttpRepositories.getResourcePath;
import static org.systemsbiology.addama.fsutils.rest.UriScheme.path;
import static org.systemsbiology.addama.fsutils.rest.UriScheme.query;
import static org.systemsbiology.google.visualization.datasource.DataSourceHelper.queryResource;

/**
 * @author hrovira
 */
@Controller
public class QueryController extends FileSystemController {

    @RequestMapping(value = "/**/query/**", method = RequestMethod.GET)
    public void query_scheme(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (contains(request.getRequestURI(), path.name())) {
            String repositoryUri = getRepositoryUri(request, path);
            String resourcePath = getResourcePath(request, path, "/query");

            Resource resource = getTargetResource(repositoryUri, resourcePath);
            queryResource(request, response, resource);
            return;
        }

        String repositoryUri = getRepositoryUri(request, query);
        String resourcePath = getResourcePath(request, query);

        assertServesFiles(repositoryUri);

        Resource resource = getTargetResource(repositoryUri, resourcePath);
        queryResource(request, response, resource);
    }
}
