package org.systemsbiology.addama.filedbcreate.callbacks;

import org.json.JSONObject;
import org.systemsbiology.addama.filedbcreate.etl.TableDDL;
import org.systemsbiology.addama.filedbcreate.etl.ddl.LabeledDataMatrixTableDDL;
import org.systemsbiology.addama.filedbcreate.etl.ddl.SimpleConfigurationTableDDL;
import org.systemsbiology.addama.filedbcreate.etl.ddl.SimpleTableTableDDL;
import org.systemsbiology.addama.jsonconfig.impls.GenericMapJsonConfigHandler;

import java.util.Map;

import static org.systemsbiology.addama.filedbcreate.etl.TableDDL.Types.valueOf;

/**
 * @author hrovira
 */
public class TableDDLJsonConfig extends GenericMapJsonConfigHandler<TableDDL> {
    private final String propertyName;

    public TableDDLJsonConfig(Map<String, TableDDL> map, String propertyName) {
        super(map, propertyName);
        this.propertyName = propertyName;
    }

    public TableDDL getSpecific(JSONObject item) throws Exception {
        if (item.has(propertyName)) {
            switch (valueOf(item.getString(propertyName))) {
                case simpleTable:
                    return new SimpleTableTableDDL();
                case labeledDataMatrix:
                    return new LabeledDataMatrixTableDDL();
                case typeMap:
                    if (item.has("schemaConfig")) {
                        return new SimpleConfigurationTableDDL(item.getJSONObject("schemaConfig"));
                    }
            }
        }
        return new SimpleTableTableDDL();
    }
}
