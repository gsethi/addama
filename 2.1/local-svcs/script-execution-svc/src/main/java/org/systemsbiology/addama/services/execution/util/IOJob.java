package org.systemsbiology.addama.services.execution.util;

import java.io.File;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class IOJob {
    private static final Logger log = Logger.getLogger(IOJob.class.getName());

    public static void mkdirs(File... dirs) {
        if (dirs != null) {
            for (File dir : dirs) {
                if (!dir.exists()) {
                    log.fine(dir.getPath() + ":" + dir.mkdirs());
                } else {
                    log.fine(dir.getPath() + ": already exists");
                }
            }
        }
    }

    public static void recursiveDelete(File f) {
        if (f.exists()) {
            if (f.isDirectory()) {
                for (File subf : f.listFiles()) {
                    recursiveDelete(subf);
                }
            }

            log.fine("deleting:" + f.getPath());
            f.delete();
        }
    }


}
