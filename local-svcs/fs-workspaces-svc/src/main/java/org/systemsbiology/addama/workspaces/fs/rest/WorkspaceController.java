package org.systemsbiology.addama.workspaces.fs.rest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.exceptions.ReadOnlyAccessException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.*;
import org.systemsbiology.addama.fsutils.util.NotStartsWithFilenameFilter;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.jsonconfig.impls.BooleanPropertyByIdMappingsHandler;
import org.systemsbiology.addama.jsonconfig.impls.StringPropertyByIdMappingsHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.fileupload.servlet.ServletFileUpload.isMultipartContent;
import static org.apache.commons.lang.StringUtils.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.*;
import static org.systemsbiology.addama.commons.web.utils.RegisteredUser.getRegistryUser;
import static org.systemsbiology.addama.commons.web.views.JsonItemsFromFilesView.*;
import static org.systemsbiology.addama.commons.web.views.ResourceFileView.RESOURCE;
import static org.systemsbiology.addama.fsutils.util.FileUtil.recurseDelete;
import static org.systemsbiology.addama.fsutils.util.FileUtil.storeInto;
import static org.systemsbiology.addama.fsutils.util.TabularData.asSchema;
import static org.systemsbiology.google.visualization.datasource.DataSourceHelper.queryResource;

/**
 * @author hrovira
 */
@Controller
public class WorkspaceController {
    private static final Logger log = Logger.getLogger(WorkspaceController.class.getName());

    private final Map<String, Mapping> mappingsById = new HashMap<String, Mapping>();
    private final HashMap<String, String> rootPathById = new HashMap<String, String>();
    private final HashMap<String, Boolean> readOnlyById = new HashMap<String, Boolean>();

    public void setServiceConfig(ServiceConfig serviceConfig) throws Exception {
        for (Mapping mapping : serviceConfig.getMappings()) {
            this.mappingsById.put(mapping.ID(), mapping);
        }
        serviceConfig.visit(new StringPropertyByIdMappingsHandler(rootPathById, "rootPath"));
        serviceConfig.visit(new BooleanPropertyByIdMappingsHandler(readOnlyById, "readOnly", false));
    }

    /*
    * Controllers
    */
    @RequestMapping(value = "/**/workspaces", method = GET)
    public ModelAndView workspaces(HttpServletRequest request) throws Exception {
        String uri = getURI(request);

        JSONObject json = new JSONObject();
        json.put("uri", uri);
        for (Mapping mapping : mappingsById.values()) {
            JSONObject item = new JSONObject();
            item.put("id", mapping.ID());
            item.put("uri", uri + "/" + mapping.ID());
            item.put("readOnly", readOnlyById.get(mapping.ID()));
            item.put("label", mapping.LABEL());
            json.append("items", item);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}", method = GET)
    public ModelAndView workspace(HttpServletRequest request,
                                  @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);

        Resource resource = getTargetResource(workspaceId, "");
        File[] files = resource.getFile().listFiles(new NotStartsWithFilenameFilter("."));

        ModelAndView mav = new ModelAndView(new JsonItemsFromFilesView());
        mav.addObject(FILES, files);
        mav.addObject(URI, uri);
        mav.addObject(READ_ONLY, readOnlyById.get(workspaceId));
        return mav;
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**", method = GET)
    public ModelAndView resource(HttpServletRequest request,
                                 @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);

        String path = substringAfterLast(uri, workspaceId);
        Resource resource = getTargetResource(workspaceId, path);

        File resourceFile = resource.getFile();
        if (resourceFile.isDirectory()) {
            File[] files = resourceFile.listFiles(new NotStartsWithFilenameFilter("."));
            return new ModelAndView(new JsonItemsFromFilesView()).addObject(FILES, files).addObject(URI, uri);
        }

        return new ModelAndView(new ResourceFileView()).addObject(RESOURCE, resource);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**", method = POST)
    public ModelAndView update(HttpServletRequest request,
                               @PathVariable("workspaceId") String workspaceId) throws Exception {
        mayWrite(workspaceId, request);

        String uri = getSpacedURI(request);
        String path = substringAfterLast(uri, workspaceId);

        if (!isMultipartContent(request)) {
            Resource r = newDirectoryResource(workspaceId, path);

            File dir = r.getFile();
            if (!dir.mkdirs()) {
                log.warning("there may have been a problem creating the directory: " + dir);
            }

            JSONObject json = new JSONObject();
            json.put("uri", uri);
            json.put("name", dir.getName());
            json.put("label", dir.getName());
            return new ModelAndView(new JsonView()).addObject("json", json);
        }

        JSONObject json = new JSONObject();
        json.put("uri", uri);

        try {
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator itr = upload.getItemIterator(request);
            while (itr.hasNext()) {
                FileItemStream itemStream = itr.next();
                if (!itemStream.isFormField()) {
                    String filename = itemStream.getName();
                    if (contains(filename, "\"")) {
                        filename = replace(filename, "\"", "");
                    }
                    if (contains(filename, "\\")) {
                        filename = replace(filename, "\\", "");
                    }

                    Resource r = newFileResource(workspaceId, path + "/" + filename);
                    storeInto(itemStream.openStream(), r.getFile());

                    json.append("items", new JSONObject().put("name", r.getFilename()));
                }
            }

            json.put("success", true);
        } catch (Exception e) {
            log.warning("unable to extract content:" + e);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**/delete", method = POST)
    public ModelAndView delete_by_post(HttpServletRequest request,
                                       @PathVariable("workspaceId") String workspaceId) throws Exception {
        mayWrite(workspaceId, request);

        String uri = getSpacedURI(request);
        String path = substringBetween(uri, workspaceId, "/delete");
        Resource resource = getTargetResource(workspaceId, path);
        recurseDelete(resource.getFile());
        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**", method = DELETE)
    public ModelAndView delete(HttpServletRequest request,
                               @PathVariable("workspaceId") String workspaceId) throws Exception {
        mayWrite(workspaceId, request);

        String uri = getSpacedURI(request);
        String path = substringAfterLast(uri, workspaceId);
        Resource resource = getTargetResource(workspaceId, path);
        recurseDelete(resource.getFile());
        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**/zip", method = GET)
    public void zip_dir(HttpServletRequest request, HttpServletResponse response,
                        @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);
        String path = substringBetween(uri, workspaceId, "/zip");
        Resource resource = getTargetResource(workspaceId, path);
        zip(response, resource);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**/zip", method = POST)
    public void zip_files(HttpServletResponse response, @PathVariable("workspaceId") String workspaceId,
                          @RequestParam("name") String name, @RequestParam("uris") String[] fileUris) throws Exception {
        Map<String, InputStream> inputStreamsByName = new HashMap<String, InputStream>();
        for (String fileUri : fileUris) {
            String path = substringAfter(fileUri, workspaceId);
            Resource resource = getTargetResource(workspaceId, path);
            File f = resource.getFile();
            if (f.isDirectory()) {
                collectFiles(inputStreamsByName, f);
            } else {
                inputStreamsByName.put(resource.getFilename(), resource.getInputStream());
            }
        }

        zip(response, name + ".zip", inputStreamsByName);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**/contents", method = GET)
    public void contents(HttpServletRequest request, HttpServletResponse response,
                         @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);
        String path = substringBetween(uri, workspaceId, "/contents");
        Resource resource = getTargetResource(workspaceId, path);
        if (!resource.exists()) {
            throw new ResourceNotFoundException(uri);
        }

        setContentType(request, response, resource.getFilename());
        pipe(resource.getInputStream(), response.getOutputStream());
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**/query", method = GET)
    public void query(HttpServletRequest request, HttpServletResponse response,
                      @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);
        String path = substringBetween(uri, workspaceId, "/query");
        Resource resource = getTargetResource(workspaceId, path);
        queryResource(request, response, resource);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**/schema", method = GET)
    public ModelAndView schema(HttpServletRequest request,
                               @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);
        String path = substringBetween(uri, workspaceId, "/schema");
        Resource resource = getTargetResource(workspaceId, path);

        Map<String, String> schema = asSchema(resource);

        JSONObject json = new JSONObject();
        for (Map.Entry<String, String> entry : schema.entrySet()) {
            JSONObject column = new JSONObject();
            column.put("name", entry.getKey());
            column.put("datatype", entry.getValue());
            json.append("items", column);
        }
        json.put("comment", "data types presented here are best guesses");

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    /*
    * Protected Methods
    */
    protected Resource getTargetResource(String workspaceId, String path) throws ResourceNotFoundException {
        log.info(workspaceId + ":" + path);
        if (isEmpty(workspaceId) || !rootPathById.containsKey(workspaceId)) {
            throw new ResourceNotFoundException(workspaceId);
        }

        if (path == null) path = "";

        if (path.startsWith("/")) {
            path = substringAfter(path, "/");
        }

        String basePath = chomp(rootPathById.get(workspaceId), "/");
        Resource r = new FileSystemResource(basePath + "/" + path);
        if (!r.exists()) {
            log.warning("resource does not exist under [" + basePath + "]");
            throw new ResourceNotFoundException(path);
        }
        return r;
    }

    protected Resource newDirectoryResource(String workspaceId, String path)
            throws ResourceNotFoundException, InvalidSyntaxException, IOException {
        log.info(workspaceId + ":" + path);
        if (isEmpty(workspaceId) || !rootPathById.containsKey(workspaceId)) {
            throw new ResourceNotFoundException(workspaceId);
        }

        if (isEmpty(path)) {
            throw new InvalidSyntaxException("path must be specified");
        }

        if (path.startsWith("/")) {
            path = substringAfter(path, "/");
        }

        String basePath = chomp(rootPathById.get(workspaceId), "/");
        File dir = new File(basePath + "/" + path);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("unable to create directory at " + path);
        }

        return new FileSystemResource(dir);
    }

    protected Resource newFileResource(String workspaceId, String path)
            throws ResourceNotFoundException, InvalidSyntaxException, IOException {
        log.info(workspaceId + ":" + path);
        if (isEmpty(workspaceId) || !rootPathById.containsKey(workspaceId)) {
            throw new ResourceNotFoundException(workspaceId);
        }

        if (isEmpty(path)) {
            throw new InvalidSyntaxException("path must be specified");
        }

        if (path.startsWith("/")) {
            path = substringAfter(path, "/");
        }

        String basePath = chomp(rootPathById.get(workspaceId), "/");
        File file = new File(basePath + "/" + path);
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException("unable to create file at " + path);
            }
        }

        return new FileSystemResource(file);
    }

    protected void mayWrite(String workspaceId, HttpServletRequest request) throws Exception {
        Mapping mapping = this.mappingsById.get(workspaceId);
        if (mapping == null) {
            throw new ResourceNotFoundException(workspaceId);
        }

        if (readOnlyById.get(workspaceId)) {
            throw new ReadOnlyAccessException(workspaceId);
        }

        if (mapping.hasWriters() && !mapping.isWriter(getRegistryUser(request))) {
            throw new ReadOnlyAccessException(workspaceId);
        }
    }
}
