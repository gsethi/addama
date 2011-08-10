package org.systemsbiology.addama.fsutils.controllers;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.jsonconfig.impls.BooleanMapJsonConfigHandler;
import org.systemsbiology.addama.jsonconfig.impls.StringMapJsonConfigHandler;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.chomp;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author hrovira
 */
public abstract class FileSystemController {
    private final HashMap<String, String> repositoryPathsByUri = new HashMap<String, String>();
    private final HashMap<String, Boolean> serveFilesByUri = new HashMap<String, Boolean>();
    private final HashMap<String, Boolean> readOnlyByUri = new HashMap<String, Boolean>();

    public void setJsonConfig(JsonConfig jsonConfig) {
        jsonConfig.visit(new BooleanMapJsonConfigHandler(serveFilesByUri, "serveFiles"));
        jsonConfig.visit(new BooleanMapJsonConfigHandler(readOnlyByUri, "readOnly"));
        jsonConfig.visit(new StringMapJsonConfigHandler(repositoryPathsByUri, "rootPath"));
    }

    /**
     * Retrieves a handle on the actual underlying file system resource
     *
     * @param repositoryUri - repository
     * @param path          - path
     * @return resource
     * @throws ResourceNotFoundException - if not found
     * @todo handle after slashes
     * @todo throw Invalid?
     */
    public Resource getTargetResource(String repositoryUri, String path) throws ResourceNotFoundException {
        if (isEmpty(repositoryUri) || !repositoryPathsByUri.containsKey(repositoryUri)) {
            throw new ResourceNotFoundException(repositoryUri);
        }

        String resourcePath = repositoryPathsByUri.get(repositoryUri);
        if (!isEmpty(path)) {
            return new FileSystemResource(resourcePath + path);
        }
        return new FileSystemResource(resourcePath);
    }

    /**
     * Returns the resource at the given uri for a workspace service
     * Resource can be for a non-existing file or directory that you wish to create
     *
     * @param uri - resource target
     * @return Resource - resource object
     * @throws ResourceNotFoundException - if repository mapping doesn't exist
     */
    public Resource getWorkspaceResource(String uri) throws ResourceNotFoundException {
        for (Map.Entry<String, String> entry : repositoryPathsByUri.entrySet()) {
            String repositoryUri = chomp(entry.getKey(), "/");
            if (uri.startsWith(repositoryUri)) {
                String repositoryPath = chomp(entry.getValue(), "/");
                return new FileSystemResource(repositoryPath + uri);
            }
        }
        throw new ResourceNotFoundException(uri);
    }

    /**
     * Verifies request can serve files
     *
     * @param repositoryUri - requested repository
     * @throws InvalidSyntaxException - bad request
     */
    public void assertServesFiles(String repositoryUri) throws InvalidSyntaxException {
        if (!allowsServingFiles(repositoryUri)) {
            throw new InvalidSyntaxException(repositoryUri + ": not authorized to serve this file. use local path");
        }
    }

    /**
     * Indicates if repository allows files to be served
     *
     * @param repositoryUri - requested repository
     * @return boolean - allows or not
     */
    public boolean allowsServingFiles(String repositoryUri) {
        return serveFilesByUri.containsKey(repositoryUri) && serveFilesByUri.get(repositoryUri);
    }

    /**
     * Verifies request can serve files
     *
     * @param uri - requested resource
     * @throws InvalidSyntaxException - bad request
     */
    public void assertAllowsWrites(String uri) throws InvalidSyntaxException, ResourceNotFoundException {
        String repositoryUri = getRepositoryUri(uri);
        Boolean readOnly = readOnlyByUri.get(repositoryUri);
        if (readOnly != null && readOnly) {
            throw new InvalidSyntaxException(repositoryUri + ": not authorized to modify");
        }
    }

    private String getRepositoryUri(String uri) throws ResourceNotFoundException {
        for (String repositoryUri : repositoryPathsByUri.keySet()) {
            if (uri.startsWith(repositoryUri)) {
                return repositoryUri;
            }
        }
        throw new ResourceNotFoundException(uri);
    }
}
