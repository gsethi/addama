package org.systemsbiology.addama.filedbcreate;

/**
 * @author hrovira
 */
public class TableBean {
    private final String uri;
    private final String tableName;

    public TableBean(String uri, String tableName) {
        this.uri = uri;
        this.tableName = tableName;
    }

    public String getUri() {
        return uri;
    }

    public String getTableName() {
        return tableName;
    }

}
