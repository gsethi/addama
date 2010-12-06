package org.systemsbiology.addama.commons.gae.dataaccess;

import java.io.Serializable;

/**
 * @author hrovira
 */
public interface MemcacheLoaderCallback {
    public Serializable getCacheableObject(String key) throws Exception;
}
