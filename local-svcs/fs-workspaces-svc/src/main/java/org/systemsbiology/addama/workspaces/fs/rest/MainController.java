package org.systemsbiology.addama.workspaces.fs.rest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
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
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;
import org.systemsbiology.addama.commons.web.views.ResourceFileView;
import org.systemsbiology.addama.fsutils.util.EndlineFixingInputStream;
import org.systemsbiology.addama.fsutils.util.NotStartsWithFilenameFilter;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.jsonconfig.impls.StringPropertyByIdMappingsHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.Double.parseDouble;
import static org.apache.commons.fileupload.servlet.ServletFileUpload.isMultipartContent;
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.*;
import static org.systemsbiology.addama.commons.web.views.ResourceFileView.RESOURCE;
import static org.systemsbiology.google.visualization.datasource.DataSourceHelper.queryResource;

/**
 * @author hrovira
 */
@Controller
public class MainController {
    private static final Logger log = Logger.getLogger(MainController.class.getName());

    private Iterable<Mapping> mappings;
    private final HashMap<String, String> rootPathById = new HashMap<String, String>();

    public void setServiceConfig(ServiceConfig serviceConfig) throws Exception {
        this.mappings = serviceConfig.getMappings();
        serviceConfig.visit(new StringPropertyByIdMappingsHandler(rootPathById, "rootPath"));
    }

    /*
    * Controllers
    */
    @RequestMapping(value = "/**/workspaces", method = RequestMethod.GET)
    public ModelAndView workspaces(HttpServletRequest request) throws Exception {
        String uri = getURI(request);

        JSONObject json = new JSONObject();
        json.put("uri", uri);
        for (Mapping mapping : mappings) {
            JSONObject item = new JSONObject();
            item.put("id", mapping.ID());
            item.put("uri", uri + "/" + mapping.ID());
            item.put("label", mapping.LABEL());
            json.append("items", item);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}", method = RequestMethod.GET)
    public ModelAndView workspace(HttpServletRequest request, @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);

        Resource resource = getTargetResource(workspaceId, "");
        File resourceFile = resource.getFile();
        JSONObject json = new JSONObject();
        for (File f : resourceFile.listFiles(new NotStartsWithFilenameFilter("."))) {
            json.append("items", fileAsJson(uri + "/" + f.getName(), f, request));
        }
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**", method = RequestMethod.GET)
    public ModelAndView get(HttpServletRequest request, @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);

        String path = substringAfterLast(uri, workspaceId);
        Resource resource = getTargetResource(workspaceId, path);

        File resourceFile = resource.getFile();
        if (resourceFile.isDirectory()) {
            JSONObject json = new JSONObject();
            json.put("uri", uri);
            for (File f : resourceFile.listFiles(new NotStartsWithFilenameFilter("."))) {
                json.append("items", fileAsJson(uri + "/" + f.getName(), f, request));
            }
            return new ModelAndView(new JsonItemsView()).addObject("json", json);
        }

        return new ModelAndView(new ResourceFileView()).addObject(RESOURCE, resource);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**", method = RequestMethod.POST)
    public ModelAndView post(HttpServletRequest request, @PathVariable("workspaceId") String workspaceId) throws Exception {
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
                    if (contains(filename, "\\")) {
                        filename = substringAfterLast(filename, "\\");
                    }

                    Resource r = getTargetResource(workspaceId, path + "/" + filename);
                    store(r, itemStream.openStream());

                    json.append("items", fileAsJson(uri + "/" + filename, r.getFile(), request));
                }
            }

            json.put("success", true);
        } catch (Exception e) {
            log.warning("unable to extract content:" + e);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**/delete", method = RequestMethod.POST)
    public ModelAndView delete_by_post(HttpServletRequest request, @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);
        String path = substringBetween(uri, workspaceId, "/delete");
        return mavDelete(workspaceId, path);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**", method = RequestMethod.DELETE)
    public ModelAndView delete(HttpServletRequest request, @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);
        String path = substringAfterLast(uri, workspaceId);
        return mavDelete(workspaceId, path);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**/zip", method = RequestMethod.GET)
    public void zipDir(HttpServletRequest request, HttpServletResponse response,
                       @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);
        String path = substringBetween(uri, workspaceId, "/zip");
        Resource resource = getTargetResource(workspaceId, path);
        zip(response, resource);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**/zip", method = RequestMethod.POST)
    public void zipFiles(HttpServletResponse response, @PathVariable("workspaceId") String workspaceId,
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

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**/contents", method = RequestMethod.GET)
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

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**/query", method = RequestMethod.GET)
    public void query_scheme(HttpServletRequest request, HttpServletResponse response,
                             @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);
        String path = substringBetween(uri, workspaceId, "/query");
        Resource resource = getTargetResource(workspaceId, path);
        queryResource(request, response, resource);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**/schema", method = RequestMethod.GET)
    public ModelAndView schema(HttpServletRequest request,
                               @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);
        String path = substringBetween(uri, workspaceId, "/schema");
        Resource resource = getTargetResource(workspaceId, path);

        Map<String, String> schema = getSchema(resource);

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
        if (dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("unable to create directory at " + path);
            }
        }

        return new FileSystemResource(dir);
    }

    /*
    * Private Methods
    */

    private ModelAndView mavDelete(String workspaceId, String path) throws Exception {
        Resource resource = getTargetResource(workspaceId, path);
        recurseDelete(resource.getFile());
        return new ModelAndView(new OkResponseView());
    }

    private void recurseDelete(File... files) {
        for (File sf : files) {
            if (sf.isDirectory()) {
                recurseDelete(sf.listFiles());
            }
            if (!sf.delete()) {
                log.warning("there may have been a problem deleting [" + sf.getPath() + "]");
            }
        }
    }

    private void store(Resource resource, InputStream inputStream) throws Exception {
        File f = resource.getFile();
        if (!f.exists()) {
            if (!f.getParentFile().mkdirs()) {
                log.warning("there may have been a problem creating directories [" + f.getParentFile() + "]");
            }
        }

        pipe_close(new EndlineFixingInputStream(inputStream), new FileOutputStream(f.getPath(), false));
    }

    private JSONObject fileAsJson(String uri, File f, HttpServletRequest request) throws JSONException, IOException {
        String filename = f.getName();

        JSONObject json = new JSONObject();
        json.put("name", filename);
        json.put("label", filename);
        json.put("uri", uri);

        boolean isFile = f.isFile();
        json.put("isFile", isFile);
        if (isFile) {
            json.put("size", f.length());
            json.put("mimeType", getMimeType(request, f));
        }
        return json;
    }

    private Map<String, String> getSchema(Resource resource) throws Exception {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String columnHeader = reader.readLine();
            String splitter = getSplitter(columnHeader);

            String[] headers = columnHeader.split(splitter);

            String firstLine = reader.readLine();
            String[] values = firstLine.split(splitter);

            if (headers.length != values.length) {
                throw new InvalidSyntaxException("number of column headers do not match number of value columns");
            }

            Map<String, String> schema = new HashMap<String, String>();
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i];
                String value = values[i];
                schema.put(header, guessedDataType(value));
            }
            return schema;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                log.warning(e.getMessage());
            }
        }
    }

    private String guessedDataType(String value) {
        if (!isEmpty(value)) {
            if (equalsIgnoreCase(value, "true") || equalsIgnoreCase(value, "false")) {
                return "boolean";
            }
            try {
                parseDouble(value);
                return "number";
            } catch (NumberFormatException e) {
                log.warning(value + ":" + e);
            }
        }
        return "string";
    }

    private String getSplitter(String columnHeader) throws InvalidSyntaxException {
        if (!isEmpty(columnHeader)) {
            if (contains(columnHeader, "\t")) {
                return "\t";
            }
            if (contains(columnHeader, ",")) {
                return ",";
            }
        }
        throw new InvalidSyntaxException("file does not seem to be tabular");
    }

}
