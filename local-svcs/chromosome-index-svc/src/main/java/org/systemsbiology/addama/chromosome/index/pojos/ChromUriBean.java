/*
**    Copyright (C) 2003-2010 Institute for Systems Biology
**                            Seattle, Washington, USA.
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
*/
package org.systemsbiology.addama.chromosome.index.pojos;

import static java.lang.Long.parseLong;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.systemsbiology.addama.chromosome.index.pojos.ChromUriBean.Strand.*;

/**
 * @author hrovira
 */
public class ChromUriBean {
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
    }

    private final String build;
    private final String chromosome;
    private final Long start;
    private final Long end;
    private final Strand strand;

    public ChromUriBean(String build, String chromosome, Long start, Long end, String sign) {
        this.build = build;
        this.chromosome = chromosome;
        this.start = start;
        this.end = end;
        this.strand = transformStrand(sign);
    }

    public ChromUriBean(String chromosomeUri) {
        String[] splits = chromosomeUri.split("/");

        this.build = getSplit(splits, 3);
        this.chromosome = getSplit(splits, 4);
        this.start = getLongSplit(splits, 5);
        this.end = getLongSplit(splits, 6);

        String sign = getSplit(splits, 7);
        this.strand = transformStrand(sign);
    }

    public String getBuild() {
        return build;
    }

    public String getChromosome() {
        return chromosome;
    }

    public Long getStart() {
        return start;
    }

    public Long getEnd() {
        return end;
    }

    public Strand getStrand() {
        return strand;
    }

    /*
     * Private Methods
     */

    private Long getLongSplit(String[] splits, int position) {
        String split = getSplit(splits, position);
        if (isEmpty(split)) {
            return null;
        }
        return parseLong(split);
    }

    private String getSplit(String[] splits, int position) {
        if (splits.length > position) {
            return splits[position];
        }
        return null;
    }

    private Strand transformStrand(String sign) {
        if (equalsIgnoreCase(sign, "+")) {
            return positive;
        }
        if (equalsIgnoreCase(sign, "-")) {
            return negative;
        }
        return unspecified;
    }

    /*
     * String
     */

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        builder.append("b=").append(this.build).append(",");
        builder.append("c=").append(this.chromosome).append(",");
        builder.append("sta=").append(this.start).append(",");
        builder.append("end=").append(this.end);
        builder.append("s=").append(this.strand);
        builder.append("]");
        return builder.toString();
    }
}