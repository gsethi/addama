package org.systemsbiology.addama.fsutils.util;

import java.io.File;
import java.io.FilenameFilter;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author hrovira
 */
public class NotStartsWithFilenameFilter implements FilenameFilter {
    private final String[] notStarts;

    public NotStartsWithFilenameFilter(String... notStarts) {
        this.notStarts = notStarts;
    }

    public boolean accept(File dir, String name) {
        if (!isEmpty(name)) {
            for (String notStart : notStarts) {
                if (name.startsWith(notStart)) {
                    return false;
                }
            }
        }
        return true;
    }
}
