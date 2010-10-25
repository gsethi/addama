package org.systemsbiology.addama.datasources.util;

import org.apache.commons.lang.StringUtils;

/**
 * @author hrovira
 */
public class TqxParser {
    private final String tqx;

    public TqxParser(String tqx) {
        this.tqx = tqx;
    }

    public String getOut() {
        if (!StringUtils.isEmpty(tqx)) {
            String[] parts = tqx.split(";");
            for (String part : parts) {
                if (part.startsWith("out")) {
                    String out = StringUtils.substringAfter(part, "out:");
                    if (!StringUtils.isEmpty(out)) {
                        return out;
                    }
                }
            }
        }
        return null;
    }
}
