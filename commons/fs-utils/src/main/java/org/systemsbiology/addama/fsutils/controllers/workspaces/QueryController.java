package org.systemsbiology.addama.fsutils.controllers.workspaces;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.systemsbiology.addama.fsutils.controllers.FileSystemController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang.StringUtils.substringBetween;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.getSpacedURI;
import static org.systemsbiology.google.visualization.datasource.DataSourceHelper.queryResource;

/**
 * @author hrovira
 */
@Controller
public class QueryController extends FileSystemController {
    @RequestMapping(value = "/**/workspaces/{workspaceId}/**/query", method = RequestMethod.GET)
    public void query_scheme(HttpServletRequest request, HttpServletResponse response,
                             @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);
        String path = substringBetween(uri, workspaceId, "/query");
        Resource resource = getTargetResource(workspaceId, path);
        queryResource(request, response, resource);
    }
}
