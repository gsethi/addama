package org.systemsbiology.addama.workspaces.callbacks;

import java.io.Closeable;

/**
 * @author hrovira
 */
public interface FileLocal extends Closeable {

    public String getLocalFile() throws Exception;

}
