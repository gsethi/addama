package org.systemsbiology.addama.appengine.memcache;

import java.io.Serializable;

/**
 * @author hrovira
 */
public interface MemcachePaginatedLoaderCallback {
    public Serializable getCacheableObject(String key, int limit, int offset) throws Exception;
}
