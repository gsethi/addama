package org.systemsbiology.addama.fsutils.controllers.workspaces;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.fsutils.controllers.FileSystemController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.systemsbiology.addama.commons.web.utils.HttpIO.getCleanUri;
import static org.systemsbiology.google.visualization.datasource.DataSourceHelper.queryResource;

/**
 * @author hrovira
 */
@Controller
public class QueryController extends FileSystemController {
    @RequestMapping(value = "/**/query", method = RequestMethod.GET)
    public void query_scheme(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = getCleanUri(request, "/query");

        Resource resource = getWorkspaceResource(uri);
        if (!resource.exists()) {
            throw new ResourceNotFoundException(uri);
        }

        queryResource(request, response, resource);
    }
}
