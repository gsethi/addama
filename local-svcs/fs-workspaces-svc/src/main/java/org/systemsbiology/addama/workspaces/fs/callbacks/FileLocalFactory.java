package org.systemsbiology.addama.workspaces.fs.callbacks;

/**
 * @author hrovira
 */
public interface FileLocalFactory {
    FileLocal getFileLocal(String databaseUri, String localPath);
}
