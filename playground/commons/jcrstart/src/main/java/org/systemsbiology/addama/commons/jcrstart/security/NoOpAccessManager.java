package org.systemsbiology.addama.commons.jcrstart.security;

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;

import javax.jcr.RepositoryException;

/**
 * @author hrovira
 */
public class NoOpAccessManager implements AccessManager {
    public void init(AMContext context) throws Exception {
    }

    public synchronized void close() throws Exception {
    }

    public void checkPermission(ItemId id, int permissions) throws RepositoryException {
    }

    public boolean isGranted(ItemId id, int permissions) throws RepositoryException {
        return true;
    }

    public boolean canAccess(String workspaceName) throws RepositoryException {
        return true;
    }
}
