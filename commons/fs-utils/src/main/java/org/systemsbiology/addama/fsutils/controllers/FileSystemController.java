package org.systemsbiology.addama.fsutils.controllers;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.jsonconfig.impls.StringPropertyByIdMappingsHandler;

import java.util.HashMap;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author hrovira
 */
public abstract class FileSystemController {
    private final HashMap<String, String> repositoryPathsById = new HashMap<String, String>();

    public void setServiceConfig(ServiceConfig serviceConfig) throws Exception {
        serviceConfig.visit(new StringPropertyByIdMappingsHandler(repositoryPathsById, "rootPath"));
    }

    /**
     * Retrieves a handle on the actual underlying file system resource
     *
     * @param repositoryId - repository
     * @param path         - path
     * @return resource
     * @throws ResourceNotFoundException - if not found
     */
    public Resource getTargetResource(String repositoryId, String path) throws ResourceNotFoundException {
        if (isEmpty(repositoryId) || !repositoryPathsById.containsKey(repositoryId)) {
            throw new ResourceNotFoundException(repositoryId);
        }

        String resourcePath = repositoryPathsById.get(repositoryId);
        if (!isEmpty(path)) {
            resourcePath += path;
        }
        Resource r = new FileSystemResource(resourcePath);
        if (!r.exists()) {
            System.out.println("resourcePath=" + resourcePath);
            throw new ResourceNotFoundException(path);
        }
        return r;
    }
}
