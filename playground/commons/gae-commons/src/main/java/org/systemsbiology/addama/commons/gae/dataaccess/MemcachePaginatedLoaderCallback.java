package org.systemsbiology.addama.commons.gae.dataaccess;

import java.io.Serializable;

/**
 * @author hrovira
 */
public interface MemcachePaginatedLoaderCallback {
    public Serializable getCacheableObject(String key, int limit, int offset) throws Exception;
}
