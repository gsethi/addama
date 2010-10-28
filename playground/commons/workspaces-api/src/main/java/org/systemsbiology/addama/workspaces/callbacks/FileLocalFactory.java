package org.systemsbiology.addama.workspaces.callbacks;

import org.systemsbiology.addama.workspaces.callbacks.FileLocal;

/**
 * @author hrovira
 */
public interface FileLocalFactory {
    FileLocal getFileLocal(String databaseUri, String localPath);
}
