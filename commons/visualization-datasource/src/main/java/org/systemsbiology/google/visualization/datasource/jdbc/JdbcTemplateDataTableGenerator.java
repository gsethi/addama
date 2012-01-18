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
package org.systemsbiology.google.visualization.datasource.jdbc;

import com.google.common.collect.Lists;
import com.google.visualization.datasource.Capabilities;
import com.google.visualization.datasource.DataTableGenerator;
import com.google.visualization.datasource.base.DataSourceException;
import com.google.visualization.datasource.base.ReasonType;
import com.google.visualization.datasource.base.TypeMismatchException;
import com.google.visualization.datasource.datatable.ColumnDescription;
import com.google.visualization.datasource.datatable.DataTable;
import com.google.visualization.datasource.datatable.TableCell;
import com.google.visualization.datasource.datatable.TableRow;
import com.google.visualization.datasource.datatable.value.*;
import com.google.visualization.datasource.query.*;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.TimeZone;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import javax.servlet.http.HttpServletRequest;
import java.sql.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author skillcoyne 6/2009
 */
public class JdbcTemplateDataTableGenerator implements DataTableGenerator {
    private static final Logger log = Logger.getLogger(JdbcTemplateDataTableGenerator.class.getName());

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private Integer maxRows;

    public JdbcTemplateDataTableGenerator(JdbcTemplate jdbcTemplate, String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
    }

    public void setMaxRows(Integer maxRows) {
        this.maxRows = maxRows;
    }

    public DataTable generateDataTable(Query query, HttpServletRequest request) throws DataSourceException {
        return executeQuery(query, tableName);
    }

    public Capabilities getCapabilities() {
        return Capabilities.SQL;
    }

    /**
     * Executes the given query on the given SQL database table, and returns the result as a DataTable.
     *
     * @param query     The query.
     * @param tableName Table name for the query to run on.
     * @return DataTable A data table with the data from the specified sql table, after applying the specified query on
     *         it.
     * @throws com.google.visualization.datasource.base.DataSourceException
     *          Thrown when the data source fails to perform the action.
     */
    public DataTable executeQuery(Query query, String tableName) throws DataSourceException {
        // Build the sql query.
        StrBuilder queryStringBuilder = new StrBuilder();
        buildSqlQuery(query, queryStringBuilder, tableName);
        List<String> columnIdsList = null;
        if (query.hasSelection())
            columnIdsList = getColumnIdsList(query.getSelection());

        final List<String> columnIds = columnIdsList;

        final DataTable dataTable = new DataTable();

        // Specify the maximum number of rows, if given.
        if (maxRows != null) {
            jdbcTemplate.setMaxRows(maxRows);
        }

        // Execute the sql query.
        jdbcTemplate.query(queryStringBuilder.toString(), new ResultSetExtractor() {
            public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
                ResultSetMetaData metaData = rs.getMetaData();
                int resultCols = metaData.getColumnCount();
                // For each column in the table, create the column description. SQL indexes
                // are 1-based.
                for (int i = 1; i <= resultCols; i++) {
                    String id = (columnIds == null) ? metaData.getColumnLabel(i) :
                            columnIds.get(i - 1);
                    ColumnDescription columnDescription =
                            new ColumnDescription(id,
                                    sqlTypeToValueType(metaData.getColumnType(i)),
                                    metaData.getColumnLabel(i));
                    dataTable.addColumn(columnDescription);
                }

                // Fill the data in the data table.
                List<ColumnDescription> columnsDescriptionList = dataTable.getColumnDescriptions();
                int tableCols = dataTable.getNumberOfColumns();

                // Get the value types of the columns.
                ValueType[] columnsTypeArray = new ValueType[tableCols];
                for (int c = 0; c < tableCols; c++)
                    columnsTypeArray[c] = columnsDescriptionList.get(c).getType();

                // Build the data table rows, and in each row create the table cells with
                // the information in the result set.
                while (rs.next()) {
                    TableRow tableRow = new TableRow();
                    for (int c = 0; c < tableCols; c++)
                        tableRow.addCell(buildTableCell(rs, columnsTypeArray[c], c));
                    try {
                        dataTable.addRow(tableRow);
                    }
                    catch (TypeMismatchException e) { /* Should not happen. An SQLException would already have been thrown if there was such a problem. */
                        log.warning(e.getMessageToUser());
                    }
                }
                return null;
            }
        });
        return dataTable;
    }

    /**
     * Builds the sql query.
     *
     * @param query              The query.
     * @param queryStringBuilder A string builder to build the sql query.
     * @param tableName          The sql table name.
     * @throws com.google.visualization.datasource.base.DataSourceException
     *          On errors to create the data table.
     */
    private void buildSqlQuery(Query query, StrBuilder queryStringBuilder, String tableName)
            throws DataSourceException {
        appendSelectClause(query, queryStringBuilder);
        appendFromClause(queryStringBuilder, tableName);
        appendWhereClause(query, queryStringBuilder);
        appendGroupByClause(query, queryStringBuilder);
        appendOrderByClause(query, queryStringBuilder);
        appendLimitAndOffsetClause(query, queryStringBuilder);
    }

    /**
     * Appends the LIMIT and OFFSET clause of the sql query to the given string builder. If there is no LIMIT on the
     * number of rows, uses the system row limit.
     *
     * @param query              The query.
     * @param queryStringBuilder The string builder holding the string query.
     */
    private void appendLimitAndOffsetClause(Query query, StrBuilder queryStringBuilder) {
        if (query.hasRowLimit()) {
            queryStringBuilder.append("LIMIT ");
            queryStringBuilder.append(query.getRowLimit());
        }
        if (query.hasRowOffset()) {
            queryStringBuilder.append(" OFFSET ").append(query.getRowOffset());
        }
    }

    /**
     * Appends the GROUP BY clause of the sql query to the given string builder.
     *
     * @param query              The query.
     * @param queryStringBuilder The string builder holding the string query.
     */
    private void appendGroupByClause(Query query, StrBuilder queryStringBuilder) {
        if (!query.hasGroup()) {
            return;
        }
        queryStringBuilder.append("GROUP BY ");
        QueryGroup queryGroup = query.getGroup();
        List<String> groupColumnIds = queryGroup.getColumnIds();
        List<String> newColumnIds = Lists.newArrayList();
        for (String groupColumnId : groupColumnIds) {
            newColumnIds.add('`' + groupColumnId + '`');
        }
        queryStringBuilder.appendWithSeparators(newColumnIds, ", ");
        queryStringBuilder.append(" ");
    }

    /**
     * Appends the ORDER BY clause of the sql query to the given string builder.
     *
     * @param query              The query.
     * @param queryStringBuilder The string builder holding the string query.
     */
    private void appendOrderByClause(Query query, StrBuilder queryStringBuilder) {
        if (!query.hasSort()) {
            return;
        }
        queryStringBuilder.append("ORDER BY ");
        QuerySort querySort = query.getSort();
        List<ColumnSort> sortColumns = querySort.getSortColumns();
        int numOfSortColumns = sortColumns.size();
        for (int col = 0; col < numOfSortColumns; col++) {
            ColumnSort columnSort = sortColumns.get(col);
            queryStringBuilder.append(getColumnId(columnSort.getColumn()));
            if (columnSort.getOrder() == SortOrder.DESCENDING) {
                queryStringBuilder.append(" DESC");
            }
            if (col < numOfSortColumns - 1) {
                queryStringBuilder.append(", ");
            }
        }
        queryStringBuilder.append(" ");
    }

    /**
     * Appends the WHERE clause of the sql query to the given string builder.
     *
     * @param query              The query.
     * @param queryStringBuilder The string builder holding the string query.
     */
    private void appendWhereClause(Query query, StrBuilder queryStringBuilder) {
        if (query.hasFilter()) {
            QueryFilter queryFilter = query.getFilter();
            queryStringBuilder.append("WHERE ")
                    .append(buildWhereClauseRecursively(queryFilter)).append(" ");
        }
    }

    /**
     * Builds the sql WHERE clause recursively from the given query filter. The WHERE clause structure is a tree where the
     * leafs are comparison filters and the internal nodes are compound filters. The recursion builds a string like an
     * in-order walk on the tree. Each filter (i.e. a node in the tree) has parenthesis around it.
     *
     * @param queryFilter The query filter.
     * @return The sql query WHERE clause as a StrBuilder.
     */
    private StrBuilder buildWhereClauseRecursively(QueryFilter queryFilter) {
        StrBuilder whereClause = new StrBuilder();

        // Base case of the recursion: the filter is a comparison filter.
        if (queryFilter instanceof ComparisonFilter) {
            buildWhereCluaseForComparisonFilter(whereClause, queryFilter);
        } else {
            // The Recursion step: queryFilter is a CompoundFilter.
            CompoundFilter compoundFilter = (CompoundFilter) queryFilter;

            int numberOfSubFilters = compoundFilter.getSubFilters().size();

            // If the compound filter is empty, build a where clause according to the
            // logical operator: nothing AND nothing -> WHERE "true", nothing OR
            // nothing -> WHERE "false" (match the query language rules).
            if (numberOfSubFilters == 0) {
                if (compoundFilter.getOperator() == CompoundFilter.LogicalOperator.AND) {
                    whereClause.append("true");
                } else {// OR
                    whereClause.append("false");
                }
            } else {
                List<String> filterComponents = Lists.newArrayList();
                for (QueryFilter filter : compoundFilter.getSubFilters()) {
                    filterComponents.add(buildWhereClauseRecursively(filter).toString());
                }
                String logicalOperator = getSqlLogicalOperator(compoundFilter.getOperator());
                whereClause.append("(").appendWithSeparators(filterComponents, " " + logicalOperator + " ")
                        .append(")");
            }
        }
        return whereClause;
    }

    /**
     * Builds the WHERE clause for comparison filter. This is the base case of the recursive building of the WHERE clause
     * of the sql query.
     *
     * @param whereClause A string builder representing the WHERE clause of the SQL query.
     * @param queryFilter The query filter.
     */
    private void buildWhereCluaseForComparisonFilter(StrBuilder whereClause, QueryFilter queryFilter) {
        StrBuilder first = new StrBuilder();
        StrBuilder second = new StrBuilder();

        // Build the left part and the right part of the clause according to the filter's type.
        if (queryFilter instanceof ColumnColumnFilter) {
            ColumnColumnFilter filter = (ColumnColumnFilter) queryFilter;
            first.append(filter.getFirstColumn().getId());
            second.append(filter.getSecondColumn().getId());
        } else { // The filter is a ColumnValueFilter
            ColumnValueFilter filter = (ColumnValueFilter) queryFilter;
            first.append(filter.getColumn().getId());
            second.append(filter.getValue().toString());
            if ((filter.getValue().getType() == ValueType.TEXT)
                    || (filter.getValue().getType() == ValueType.DATE)
                    || (filter.getValue().getType() == ValueType.DATETIME)
                    || (filter.getValue().getType() == ValueType.TIMEOFDAY)) {
                second.insert(0, "\"");
                second.insert(second.length(), "\"");
            }
        }
        whereClause.append(buildWhereClauseFromRightAndLeftParts(
                first, second, ((ComparisonFilter) queryFilter).getOperator()));
    }

    /**
     * Returns the sql operator of the given CompoundFilter.LogicalOperator as a string.
     *
     * @param operator The CompoundFilter.LogicalOperator.
     * @return A string representation of the SQL operator.
     */
    private String getSqlLogicalOperator(CompoundFilter.LogicalOperator operator) {
        String stringOperator;
        switch (operator) {
            case AND:
                stringOperator = "AND";
                break;
            case OR:
                stringOperator = "OR";
                break;
            default:// Should never get here.
                throw new RuntimeException("Logical operator was not found: " + operator);
        }
        return stringOperator;
    }

    /**
     * Builds the where clause of the SQL query sql from the two given values and the operator between these two values.
     *
     * @param value1   The first value in the where clause (either column id or value)
     * @param value2   The second value in the where clause (either column id or value)
     * @param operator The ComparisonFilter.Operator.
     * @return A string builder representing the where clause of the SQL query.
     */
    private StrBuilder buildWhereClauseFromRightAndLeftParts(
            StrBuilder value1, StrBuilder value2, ComparisonFilter.Operator operator) {
        StrBuilder clause;
        switch (operator) {
            case EQ:
                clause = value1.append("=").append(value2);
                break;
            case NE:
                clause = value1.append("<>").append(value2);
                break;
            case LT:
                clause = value1.append("<").append(value2);
                break;
            case GT:
                clause = value1.append(">").append(value2);
                break;
            case LE:
                clause = value1.append("<=").append(value2);
                break;
            case GE:
                clause = value1.append(">=").append(value2);
                break;
            case CONTAINS:
                value2 = new StrBuilder(value2.toString().replace("\"", ""));
                clause = value1.append(" LIKE ").append("\"%").append(value2).append("%\"");
                break;
            case STARTS_WITH:
                value2 = new StrBuilder(value2.toString().replace("\"", ""));
                clause = value1.append(" LIKE ").append("\"").append(value2).append("%\"");
                break;
            case ENDS_WITH:
                value2 = new StrBuilder(value2.toString().replace("\"", ""));
                clause = value1.append(" LIKE ").append("\"%").append(value2).append("\"");
                break;
            case MATCHES:
                throw new RuntimeException("SQL does not support regular expression");
            case LIKE:
                value2 = new StrBuilder(value2.toString().replace("\"", ""));
                clause = value1.append(" LIKE ").append("\"").append(value2).append("\"");
                break;
            default:// Should never get here.
                throw new RuntimeException("Operator was not found: " + operator);
        }
        clause.insert(0, "(").append(")");
        return clause;
    }

    /**
     * Appends the SELECT clause of the sql query to the given string builder.
     *
     * @param query              The query.
     * @param queryStringBuilder The string builder holding the string query.
     */

    private void appendSelectClause(Query query, StrBuilder queryStringBuilder) {
        queryStringBuilder.append("SELECT ");

        // If it's a selectAll query, build "select *" clause.
        if (!query.hasSelection()) {
            queryStringBuilder.append("* ");
            return;
        }

        List<AbstractColumn> columns = query.getSelection().getColumns();
        int numOfColsInQuery = columns.size();

        // Add the Ids of the columns to the select clause
        for (int col = 0; col < numOfColsInQuery; col++) {
            queryStringBuilder.append(getColumnId(columns.get(col)));
            if (col < numOfColsInQuery - 1) {
                queryStringBuilder.append(", ");
            }
        }
        queryStringBuilder.append(" ");
    }

    /**
     * Returns the column id in SQL.
     *
     * @param abstractColumn The column.
     * @return The column id for the data table.
     */
    private StrBuilder getColumnId(AbstractColumn abstractColumn) {
        StrBuilder columnId = new StrBuilder();

        // For simple column the id is simply the column id.
        if (abstractColumn instanceof SimpleColumn) {
            columnId.append("`").append(abstractColumn.getId()).append("`");
        } else {
            // For aggregation column build the id from the aggregation type and the
            // column id (e.g. for aggregation type 'min' and column id 'salary', the
            // sql column id will be: min(`salary`);
            AggregationColumn aggregationColumn = (AggregationColumn) abstractColumn;
            columnId.append(getAggregationFunction(
                    aggregationColumn.getAggregationType())).append("(`").
                    append(aggregationColumn.getAggregatedColumn()).append("`)");
        }
        return columnId;
    }

    /**
     * Returns a list with the selected column ids in the table description.
     *
     * @param selection The query selection.
     * @return a list with the selected column ids.
     */
    private List<String> getColumnIdsList(QuerySelection selection) {
        List<String> columnIds =
                Lists.newArrayListWithCapacity(selection.getColumns().size());
        for (AbstractColumn column : selection.getColumns()) {
            columnIds.add(column.getId());
        }
        return columnIds;
    }

    /**
     * Returns a string representation of the given aggregation type.
     *
     * @param type The aggregation type.
     * @return The aggragation type's string representation.
     */
    private String getAggregationFunction(AggregationType type) {
        return type.getCode();
    }

    /**
     * Appends the FROM clause of the sql query to the given string builder. Takes the table name from the configuration
     * file. If no table name is given, takes the table name from the query.
     *
     * @param queryStringBuilder The string builder holding the string query.
     * @param tableName          The database table name.
     * @throws com.google.visualization.datasource.base.DataSourceException
     *          Thrown when no table name provided, or when the table name in the data source doesn't
     *          match the table name in the query.
     */
    private void appendFromClause(StrBuilder queryStringBuilder, String tableName)
            throws DataSourceException {
        if (StringUtils.isEmpty(tableName)) {
            log.warning("No table name provided");
            throw new DataSourceException(ReasonType.OTHER, "No table name provided.");
        }
        queryStringBuilder.append("FROM ");
        queryStringBuilder.append(tableName);
        queryStringBuilder.append(" ");
    }

    /**
     * Converts the given SQL type to a value type.
     *
     * @param sqlType The sql type to be converted.
     * @return The value type that fits the given sql type.
     */
    private ValueType sqlTypeToValueType(int sqlType) {
        ValueType valueType;
        switch (sqlType) {
            case Types.BOOLEAN:
            case Types.BIT: {
                valueType = ValueType.BOOLEAN;
                break;
            }
            case Types.CHAR:
            case Types.VARCHAR:
                valueType = ValueType.TEXT;
                break;
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.BIGINT:
            case Types.TINYINT:
            case Types.REAL:
            case Types.NUMERIC:
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.DECIMAL:
                valueType = ValueType.NUMBER;
                break;
            case Types.DATE:
                valueType = ValueType.DATE;
                break;
            case Types.TIME:
                valueType = ValueType.TIMEOFDAY;
                break;
            case Types.TIMESTAMP:
                valueType = ValueType.DATETIME;
                break;
            default:
                valueType = ValueType.TEXT;
                break;
        }
        return valueType;
    }

    /**
     * Creates a table cell from the value in the current row of the given result set and the given column index. The type
     * of the value is determined by the given value type.
     *
     * @param rs        The result set holding the data from the sql table. The result points to the current row.
     * @param valueType The value type of the column that the cell belongs to.
     * @param column    The column index. Indexes are 0-based.
     * @return The table cell.
     * @throws java.sql.SQLException Thrown when the connection to the database failed.
     */
    private TableCell buildTableCell(ResultSet rs, ValueType valueType, int column) throws SQLException {
        Value value = null;

        // SQL indexes are 1- based.
        column = column + 1;

        switch (valueType) {
            case BOOLEAN:
                value = BooleanValue.getInstance(rs.getBoolean(column));
                break;
            case NUMBER:
                value = new NumberValue(rs.getDouble(column));
                break;
            case DATE:
                Date date = rs.getDate(column);
                // If date is null it is handled later.
                if (date != null) {
                    GregorianCalendar gc =
                            new GregorianCalendar(TimeZone.getTimeZone("GMT"));
                    // Set the year, month and date in the gregorian calendar.
                    // Use the 'set' method with those parameters, and not the 'setTime'
                    // method with the date parameter, since the Date object contains the
                    // current time zone and it's impossible to change it to 'GMT'.
                    gc.setTime(date);
                    value = new DateValue(gc);
                }
                break;
            case DATETIME:
                Timestamp timestamp = rs.getTimestamp(column);
                // If timestamp is null it is handled later.
                if (timestamp != null) {
                    GregorianCalendar gc =
                            new GregorianCalendar(TimeZone.getTimeZone("GMT"));
                    gc.setTime(timestamp);
                    // Set the milliseconds explicitly, as they are not saved in the
                    // underlying date.
                    gc.set(Calendar.MILLISECOND, timestamp.getNanos() / 1000000);
                    value = new DateTimeValue(gc);
                }
                break;
            case TIMEOFDAY:
                Time time = rs.getTime(column);
                // If time is null it is handled later.
                if (time != null) {
                    GregorianCalendar gc =
                            new GregorianCalendar(TimeZone.getTimeZone("GMT"));
                    gc.setTime(time);
                    value = new TimeOfDayValue(gc);
                }
                break;
            default:
                String colValue = rs.getString(column);
                if (colValue == null) {
                    value = TextValue.getNullValue();
                } else {
                    value = new TextValue(rs.getString(column));
                }
                break;
        }
        // Handle null values.
        if (rs.wasNull()) {
            return new TableCell(Value.getNullValueFromValueType(valueType));
        } else {
            return new TableCell(value);
        }
    }

}