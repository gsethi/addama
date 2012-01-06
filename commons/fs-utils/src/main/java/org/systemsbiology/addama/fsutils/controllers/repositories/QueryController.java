package org.systemsbiology.addama.fsutils.controllers.repositories;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang.StringUtils.substringBetween;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.getSpacedURI;
import static org.systemsbiology.google.visualization.datasource.DataSourceHelper.queryResource;

/**
 * @author hrovira
 */
@Controller
public class QueryController extends AbstractRepositoriesController {

    @RequestMapping(value = "/**/repositories/{repositoryId}/path/**/query", method = RequestMethod.GET)
    public void query_scheme(HttpServletRequest request, HttpServletResponse response,
                             @PathVariable("repositoryId") String repositoryId) throws Exception {
        assertServesFiles(repositoryId);
        String uri = getSpacedURI(request);
        String resourcePath = substringBetween(uri, "/path", "/query");
        Resource resource = getTargetResource(repositoryId, resourcePath);
        queryResource(request, response, resource);
    }
}
