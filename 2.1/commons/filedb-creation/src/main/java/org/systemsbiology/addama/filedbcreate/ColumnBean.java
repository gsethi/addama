package org.systemsbiology.addama.filedbcreate;

/**
 * @author hrovira
 */
public class ColumnBean {
    private final String tableUri;
    private final String name;
    private final String label;
    private final String type;

    public ColumnBean(TableBean table, String name, String label, String type) {
        this.tableUri = table.getUri();
        this.name = name;
        this.label = label;
        this.type = type;
    }

    public String getTableUri() {
        return tableUri;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }
}
