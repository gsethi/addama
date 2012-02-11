package org.systemsbiology.addama.fsutils.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Logger;

import static org.systemsbiology.addama.commons.web.utils.HttpIO.pipe_close;

/**
 * @author hrovira
 */
public class FileUtil {
    private static final Logger log = Logger.getLogger(FileUtil.class.getName());

    public static void recurseDelete(File... files) {
        for (File sf : files) {
            if (sf.isDirectory()) {
                recurseDelete(sf.listFiles());
            }
            if (!sf.delete()) {
                log.warning("there may have been a problem deleting [" + sf.getPath() + "]");
            }
        }
    }

    public static void storeInto(InputStream inputStream, File f) throws Exception {
        if (!f.exists()) {
            if (!f.getParentFile().mkdirs()) {
                log.warning("there may have been a problem creating directories [" + f.getParentFile() + "]");
            }
        }

        pipe_close(new EndlineFixingInputStream(inputStream), new FileOutputStream(f.getPath(), false));
    }
}
