package org.systemsbiology.addama.chromosome.index.pojos;

import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;

/**
 * @author hrovira
 */
public enum Strand {
    positive, negative, unspecified;

    public String getSign() {
        switch (this) {
            case positive:
                return "+";
            case negative:
                return "-";
        }
        return null;
    }

    public static Strand newStrand(String sign) {
        if (equalsIgnoreCase(sign, "+")) {
            return positive;
        }
        if (equalsIgnoreCase(sign, "-")) {
            return negative;
        }
        return unspecified;
    }
}
