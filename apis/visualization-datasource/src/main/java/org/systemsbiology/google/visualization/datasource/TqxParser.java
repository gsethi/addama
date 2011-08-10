package org.systemsbiology.google.visualization.datasource;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.substringAfter;

/**
 * @author hrovira
 */
public class TqxParser {
    private final String tqx;

    public TqxParser(String tqx) {
        this.tqx = tqx;
    }

    public String getOut() {
        if (!isEmpty(tqx)) {
            String[] parts = tqx.split(";");
            for (String part : parts) {
                if (part.startsWith("out")) {
                    String out = substringAfter(part, "out:");
                    if (!isEmpty(out)) {
                        return out;
                    }
                }
            }
        }
        return null;
    }
}
