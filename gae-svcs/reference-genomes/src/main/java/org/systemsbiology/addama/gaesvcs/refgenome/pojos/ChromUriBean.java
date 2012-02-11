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
package org.systemsbiology.addama.gaesvcs.refgenome.pojos;

import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;

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

    private final String uri;
    private final String build;
    private final String chromosome;
    private final Long start;
    private final Long end;
    private final Strand strand;

    public ChromUriBean(HttpServletRequest request) {
        this.uri = request.getRequestURI();

        String chromosomeUri = StringUtils.substringAfterLast(this.uri, request.getServletPath());

        int i = 1;
        String[] splits = chromosomeUri.split("/");
        this.build = getSplit(splits, i++);
        this.chromosome = getSplit(splits, i++);
        this.start = getLongSplit(splits, i++);
        this.end = getLongSplit(splits, i++);

        String sign = getSplit(splits, i++);
        if (StringUtils.equals(sign, "+")) {
            this.strand = Strand.positive;
        } else if (StringUtils.equals(sign, "-")) {
            this.strand = Strand.negative;
        } else {
            this.strand = Strand.unspecified;
        }
    }

    public String getUri() {
        return uri;
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
        if (StringUtils.isEmpty(split)) {
            return null;
        }
        return Long.parseLong(split);
    }

    private String getSplit(String[] splits, int position) {
        if (splits.length > position) {
            return splits[position];
        }
        return null;
    }

    /*
     * String
     */

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        builder.append("u=").append(this.uri).append(",");
        builder.append("b=").append(this.build).append(",");
        builder.append("c=").append(this.chromosome).append(",");
        builder.append("sta=").append(this.start).append(",");
        builder.append("end=").append(this.end).append(",");
        builder.append("s=").append(this.strand);
        builder.append("]");
        return builder.toString();
    }
}
