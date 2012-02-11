package org.systemsbiology.addama.repositories.fs.rest;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.jsonconfig.impls.BooleanPropertyByIdMappingsHandler;
import org.systemsbiology.addama.jsonconfig.impls.StringPropertyByIdMappingsHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.*;
import static org.systemsbiology.addama.fsutils.util.RangeHeaderUtil.outputResource;
import static org.systemsbiology.google.visualization.datasource.DataSourceHelper.queryResource;

/**
 * @author hrovira
 */
@Controller
public class MainController {
    private static final Logger log = Logger.getLogger(MainController.class.getName());

    private final HashMap<String, String> repositoryPathsById = new HashMap<String, String>();
    private final HashMap<String, Boolean> serveFilesById = new HashMap<String, Boolean>();

    public void setServiceConfig(ServiceConfig serviceConfig) throws Exception {
        serviceConfig.visit(new StringPropertyByIdMappingsHandler(repositoryPathsById, "rootPath"));
        serviceConfig.visit(new BooleanPropertyByIdMappingsHandler(serveFilesById, "serveFiles", true));
    }

    @RequestMapping(value = "/**/repositories/{repositoryId}", method = RequestMethod.GET)
    public ModelAndView get(HttpServletRequest request, @PathVariable("repositoryId") String repositoryId) throws Exception {
        String uri = getURI(request);
        File f = getTargetResource(repositoryId, "").getFile();
        JSONObject json = getJson(uri, f);
        appendItems(json, f);
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/repositories/{repositoryId}/path/**", method = RequestMethod.GET)
    public ModelAndView path(HttpServletRequest request, @PathVariable("repositoryId") String repositoryId) throws Exception {
        String uri = getSpacedURI(request);
        String path = substringAfterLast(uri, "/path");
        File f = getTargetResource(repositoryId, path).getFile();
        JSONObject json = getJson(uri, f);
        appendItems(json, f);
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/repositories/{repositoryId}/contents/**", method = RequestMethod.GET)
    public void contents(HttpServletRequest request, HttpServletResponse response,
                         @PathVariable("repositoryId") String repositoryId) throws Exception {
        assertServesFiles(repositoryId);

        String uri = getSpacedURI(request);
        String resourcePath = substringAfterLast(uri, "/contents");
        Resource resource = getTargetResource(repositoryId, resourcePath);

        setContentType(request, response, resource.getFilename());
        pipe(resource.getInputStream(), response.getOutputStream());
    }

    @RequestMapping(value = "/**/repositories/{repositoryId}/path/**/query", method = RequestMethod.GET)
    public void query_scheme(HttpServletRequest request, HttpServletResponse response,
                             @PathVariable("repositoryId") String repositoryId) throws Exception {
        assertServesFiles(repositoryId);
        String uri = getSpacedURI(request);
        String path = substringBetween(uri, "/path", "/query");
        Resource resource = getTargetResource(repositoryId, path);
        queryResource(request, response, resource);
    }

    @RequestMapping(value = "/**/repositories/{repositoryId}/file/**", method = RequestMethod.GET)
    public void file(HttpServletRequest request, HttpServletResponse response,
                     @PathVariable("repositoryId") String repositoryId) throws Exception {
        assertServesFiles(repositoryId);

        String uri = getSpacedURI(request);
        String path = substringAfterLast(uri, "/file");
        Resource resource = getTargetResource(repositoryId, path);
        outputResource(request, response, resource);
    }

    @RequestMapping(value = "/**/repositories/{repositoryId}/path/**/zip", method = RequestMethod.GET)
    public void zipDir(HttpServletRequest request, HttpServletResponse response,
                       @PathVariable("repositoryId") String repositoryId) throws Exception {
        assertServesFiles(repositoryId);

        String uri = getSpacedURI(request);
        String resourcePath = substringBetween(uri, "/path", "/zip");
        Resource r = getTargetResource(repositoryId, resourcePath);
        zip(response, r);
    }

    @RequestMapping(value = "/**/repositories/{repositoryId}/path/**/zip", method = RequestMethod.POST)
    public void zipFiles(HttpServletResponse response,
                         @PathVariable("repositoryId") String repositoryId,
                         @RequestParam("name") String name, @RequestParam("uris") String[] fileUris) throws Exception {
        assertServesFiles(repositoryId);

        Map<String, InputStream> inputStreamsByName = new HashMap<String, InputStream>();
        for (String fileUri : fileUris) {
            try {
                String localPath = chomp(substringAfter(fileUri, "/path"), "/");
                Resource resource = getTargetResource(repositoryId, localPath);
                File f = resource.getFile();
                if (f.isDirectory()) {
                    collectFiles(inputStreamsByName, f);
                } else {
                    inputStreamsByName.put(resource.getFilename(), resource.getInputStream());
                }
            } catch (ResourceNotFoundException e) {
                log.warning("skipping:" + e.getMessage());
            }
        }

        zip(response, name + ".zip", inputStreamsByName);
    }

    /*
     * Protected Methods
     */
    protected void assertServesFiles(String repositoryId) throws InvalidSyntaxException {
        if (!serveFilesById.get(repositoryId)) {
            throw new InvalidSyntaxException(repositoryId + ": not authorized to serve this file. use local path");
        }
    }

    protected Resource getTargetResource(String repositoryId, String path) throws ResourceNotFoundException {
        if (isEmpty(repositoryId) || !repositoryPathsById.containsKey(repositoryId)) {
            throw new ResourceNotFoundException(repositoryId);
        }

        if (path == null) path = "";

        if (path.startsWith("/")) {
            path = substringAfter(path, "/");
        }

        String basePath = chomp(repositoryPathsById.get(repositoryId), "/");
        Resource r = new FileSystemResource(basePath + "/" + path);
        if (!r.exists()) {
            log.warning("resource does not exist under [" + basePath + "]");
            throw new ResourceNotFoundException(path);
        }
        return r;
    }

    /*
     * Private Methods
     */

    private JSONObject getJson(String uri, File f) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uri", uri);
        json.put("isFile", f.isFile());
        json.put("name", f.getName());

        if (f.isFile() && contains(uri, "path")) {
            json.put("file", replace(uri, "path", "file"));
            json.put("contents", replace(uri, "path", "contents"));
            json.put("zip", uri + "/zip");
        }

        return json;
    }

    private void appendItems(JSONObject parent, File f) throws JSONException {
        if (f.isDirectory()) {
            for (File subfile : f.listFiles()) {
                String childUri = getSubUri(parent.getString("uri"), subfile);
                parent.append("items", getJson(childUri, subfile));
            }
        }
    }

    private String getSubUri(String uri, File f) {
        if (contains(uri, "path")) {
            return uri + "/" + f.getName();
        }
        return uri + "/path/" + f.getName();
    }

}
