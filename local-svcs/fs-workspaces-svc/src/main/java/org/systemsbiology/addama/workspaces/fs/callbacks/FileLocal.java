package org.systemsbiology.addama.workspaces.fs.callbacks;

import java.io.Closeable;

/**
 * @author hrovira
 */
public interface FileLocal extends Closeable {

    public String getLocalFile() throws Exception;

}
