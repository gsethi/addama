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
package org.systemsbiology.addama.chromosome.index.callbacks;

import org.json.JSONException;
import org.systemsbiology.addama.chromosome.index.pojos.QueryParams;
import org.systemsbiology.addama.chromosome.index.pojos.Schema;
import org.systemsbiology.google.visualization.datasource.jdbc.SingleStringResultSetExtractor;

import java.util.ArrayList;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.systemsbiology.addama.chromosome.index.pojos.Strand.unspecified;

/**
 * @author hrovira
 */
public class RangeQueryDistinctGenesResultSetExtractor extends SingleStringResultSetExtractor {
    private final Schema schema;
    private final QueryParams queryParams;

    public RangeQueryDistinctGenesResultSetExtractor(Schema schema, QueryParams qp) throws JSONException {
        this.schema = schema;
        this.queryParams = qp;
    }

    public String PS() {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT DISTINCT ").append(schema.getGeneIdentifierColumn());
        builder.append("  FROM ").append(schema.getTableName());
        builder.append(" WHERE ").append(schema.getChromosomeColumn()).append(" = ? ");
        if (!isEmpty(schema.getStartColumn())) {
            builder.append("   AND ").append(schema.getStartColumn()).append(" >= ? ");
        }
        if (!isEmpty(schema.getEndColumn())) {
            builder.append("   AND ").append(schema.getEndColumn()).append(" <= ? ");
        }
        if (!unspecified.equals(queryParams.getStrand())) {
            builder.append("   AND ").append(schema.getStrandColumn()).append(" = ? ");
        }
        return builder.toString();
    }

    public Object[] ARGS() {
        ArrayList<Object> args = new ArrayList<Object>();
        args.add(queryParams.getChromosome());
        if (!isEmpty(schema.getStartColumn())) {
            args.add(queryParams.getStart());
        }
        if (!isEmpty(schema.getEndColumn())) {
            args.add(queryParams.getEnd());
        }
        if (!unspecified.equals(queryParams.getStrand())) {
            args.add(queryParams.getStrand().getSign());
        }
        return args.toArray(new Object[args.size()]);
    }
}
