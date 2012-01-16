package org.systemsbiology.addama.chromosome.index.pojos;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author hrovira
 */
public class Schema {
    private final String tableName;
    private final String chromosomeColumn;
    private final String startColumn;
    private final String endColumn;
    private final String strandColumn;
    private final String geneIdentifierColumn;

    public Schema(JSONObject schema) throws JSONException {
        this.tableName = schema.getString("table");
        this.chromosomeColumn = schema.optString("chromosome", "chrom");
        this.startColumn = schema.optString("start", "start");
        this.endColumn = schema.optString("end", "end");
        this.strandColumn = schema.optString("strand", "strand");
        this.geneIdentifierColumn = schema.optString("geneIdentifier", "genes");
    }

    public String getTableName() {
        return tableName;
    }

    public String getChromosomeColumn() {
        return chromosomeColumn;
    }

    public String getStartColumn() {
        return startColumn;
    }

    public String getEndColumn() {
        return endColumn;
    }

    public String getGeneIdentifierColumn() {
        return geneIdentifierColumn;
    }

    public String getStrandColumn() {
        return strandColumn;
    }


}
