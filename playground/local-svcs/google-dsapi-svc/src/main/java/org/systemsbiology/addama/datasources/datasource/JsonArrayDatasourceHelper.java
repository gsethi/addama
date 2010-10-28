package org.systemsbiology.addama.datasources.datasource;

import com.google.visualization.datasource.*;
import com.google.visualization.datasource.base.*;
import com.google.visualization.datasource.datatable.ColumnDescription;
import com.google.visualization.datasource.datatable.DataTable;
import com.google.visualization.datasource.datatable.TableCell;
import com.google.visualization.datasource.datatable.TableRow;
import com.google.visualization.datasource.datatable.value.*;
import com.google.visualization.datasource.render.EscapeUtil;
import com.ibm.icu.util.GregorianCalendar;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class JsonArrayDatasourceHelper {
    private static final Logger log = Logger.getLogger(JsonArrayDatasourceHelper.class.getName());

    private JsonArrayDatasourceHelper() {
    }

    public static void executeDataSourceServletFlow(HttpServletRequest req, HttpServletResponse resp, DataTableGenerator dtGenerator) throws Exception {
        // Extract the data source request parameters.
        DataSourceRequest dsRequest = null;
        try {
            dsRequest = new DataSourceRequest(req);
            QueryPair query = DataSourceHelper.splitQuery(dsRequest.getQuery(), dtGenerator.getCapabilities());
            DataTable dataTable = dtGenerator.generateDataTable(query.getDataSourceQuery(), req);
            DataTable newDataTable = DataSourceHelper.applyQuery(query.getCompletionQuery(), dataTable, dsRequest.getUserLocale());

            JSONArray responseJson = generateResponse(newDataTable);

            DataSourceParameters dataSourceParameters = dsRequest.getDataSourceParameters();
            ResponseWriter.setServletResponse(responseJson.toString(), dataSourceParameters, resp);

        } catch (DataSourceException e) {
            if (dsRequest != null) {
                DataSourceHelper.setServletErrorResponse(e, dsRequest, resp);
            } else {
                DataSourceHelper.setServletErrorResponse(e, req, resp);
            }
        } catch (RuntimeException e) {
            log.warning("A runtime exception has occured:" + e);
            ResponseStatus status = new ResponseStatus(StatusType.ERROR, ReasonType.INTERNAL_ERROR, e.getMessage());
            if (dsRequest == null) {
                dsRequest = DataSourceRequest.getDefaultDataSourceRequest(req);
            }
            DataSourceHelper.setServletErrorResponse(status, dsRequest, resp);
        }
    }


    /*
    * Private Methods
    */

    private static JSONArray generateResponse(DataTable data) throws Exception {
        JSONArray jsonArray = new JSONArray();
        if (!data.getWarnings().isEmpty()) {
            return jsonArray;
        }

        if (data.getColumnDescriptions().isEmpty()) {
            return jsonArray;
        }

        for (TableRow tableRow : data.getRows()) {
            JSONObject jsonRow = new JSONObject();
            List<TableCell> cells = tableRow.getCells();
            for (int i = 0; i < cells.size(); i++) {
                ColumnDescription cd = data.getColumnDescription(i);
                TableCell cell = cells.get(i);
                jsonRow.put(cd.getLabel(), getCellValue(cell));
            }
            jsonArray.put(jsonRow);
        }
        return jsonArray;
    }

    private static Object getCellValue(TableCell cell) {
        Value value = cell.getValue();
        ValueType type = cell.getType();

        if ((value == null) || (value.isNull())) {
            return "null";
        } else {
            switch (type) {
                case BOOLEAN:
                    return ((BooleanValue) value).getValue();

                case DATE:
                    StringBuilder dtbuilder = new StringBuilder();
                    dtbuilder.append("new Date(");
                    DateValue dateValue = (DateValue) value;
                    dtbuilder.append(dateValue.getYear()).append(",");
                    dtbuilder.append(dateValue.getMonth()).append(",");
                    dtbuilder.append(dateValue.getDayOfMonth());
                    dtbuilder.append(")");
                    return dtbuilder.toString();
                case NUMBER:
                    NumberValue numval = (NumberValue) value;
                    Number number = numval.getObjectToFormat();
                    Double dval = number.doubleValue();
                    Long lval = number.longValue();
                    if ((dval - lval) == 0) {
                        return lval;
                    }
                    return dval;
                case TEXT:
                    return EscapeUtil.jsonEscape(value.toString());

                case TIMEOFDAY:
                    StringBuilder todBuilder = new StringBuilder();
                    TimeOfDayValue timeOfDayValue = (TimeOfDayValue) value;
                    todBuilder.append("[");
                    todBuilder.append(timeOfDayValue.getHours()).append(",");
                    todBuilder.append(timeOfDayValue.getMinutes()).append(",");
                    todBuilder.append(timeOfDayValue.getSeconds()).append(",");
                    todBuilder.append(timeOfDayValue.getMilliseconds());
                    todBuilder.append("]");
                    return todBuilder.toString();
                case DATETIME:
                    GregorianCalendar calendar = ((DateTimeValue) value).getCalendar();
                    StringBuilder calbuilder = new StringBuilder();
                    calbuilder.append("new Date(");
                    calbuilder.append(calendar.get(GregorianCalendar.YEAR)).append(",");
                    calbuilder.append(calendar.get(GregorianCalendar.MONTH)).append(",");
                    calbuilder.append(calendar.get(GregorianCalendar.DAY_OF_MONTH));
                    calbuilder.append(",");
                    calbuilder.append(calendar.get(GregorianCalendar.HOUR_OF_DAY));
                    calbuilder.append(",");
                    calbuilder.append(calendar.get(GregorianCalendar.MINUTE)).append(",");
                    calbuilder.append(calendar.get(GregorianCalendar.SECOND));
                    calbuilder.append(")");
                    return calbuilder.toString();
                default:
                    throw new IllegalArgumentException("Illegal value Type " + type);
            }
        }
    }
}
