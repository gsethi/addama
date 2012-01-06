package org.systemsbiology.addama.indexes.handlers;

import org.springframework.core.io.FileSystemResource;
import org.springmodules.lucene.index.support.FSDirectoryFactoryBean;

import java.io.File;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class FSDirectory {
    private static final Logger log = Logger.getLogger(FSDirectory.class.getName());

    public static FSDirectoryFactoryBean getFSDirectory(String location) throws Exception {
        File f = new File(location);
        if (!f.exists()) {
            if (f.mkdirs()) {
                log.info("creating directory for index store at: " + location);
            }
        }

        FSDirectoryFactoryBean fsDirectory = new FSDirectoryFactoryBean();
        fsDirectory.setLocation(new FileSystemResource(location));
        fsDirectory.afterPropertiesSet();
        return fsDirectory;
    }
}
