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

import static org.systemsbiology.addama.chromosome.index.pojos.Strand.newStrand;
import static org.systemsbiology.addama.chromosome.index.pojos.Strand.unspecified;

/**
 * @author hrovira
 */
public class QueryParams {
    private final String build;
    private final String chromosome;
    private final Long start;
    private final Long end;
    private final Strand strand;

    public QueryParams(String build, String chromosome) {
        this.build = build;
        this.chromosome = chromosome;
        this.start = null;
        this.end = null;
        this.strand = unspecified;
    }

    public QueryParams(String build, String chromosome, Long start, Long end) {
        this.build = build;
        this.chromosome = chromosome;
        this.start = start;
        this.end = end;
        this.strand = unspecified;
    }

    public QueryParams(String build, String chromosome, Long start, Long end, String sign) {
        this.build = build;
        this.chromosome = chromosome;
        this.start = start;
        this.end = end;
        this.strand = newStrand(sign);
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
}