package org.systemsbiology.addama.fsutils.controllers.repositories;

import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.fsutils.controllers.FileSystemController;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.jsonconfig.impls.BooleanPropertyByIdMappingsHandler;

import java.util.HashMap;

/**
 * @author hrovira
 */
public abstract class AbstractRepositoriesController extends FileSystemController {
    private final HashMap<String, Boolean> serveFilesById = new HashMap<String, Boolean>();

    public void setServiceConfig(ServiceConfig serviceConfig) throws Exception {
        super.setServiceConfig(serviceConfig);
        serviceConfig.visit(new BooleanPropertyByIdMappingsHandler(serveFilesById, "serveFiles", true));
    }

    /**
     * Verifies request can serve files
     *
     * @param repositoryId - requested repository
     * @throws org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException
     *          - bad request
     */
    public void assertServesFiles(String repositoryId) throws InvalidSyntaxException {
        if (!serveFilesById.get(repositoryId)) {
            throw new InvalidSyntaxException(repositoryId + ": not authorized to serve this file. use local path");
        }
    }
}
