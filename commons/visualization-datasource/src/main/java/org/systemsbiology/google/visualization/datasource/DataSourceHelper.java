package org.systemsbiology.google.visualization.datasource;

import com.google.visualization.datasource.DataSourceRequest;
import com.google.visualization.datasource.DataTableGenerator;
import com.google.visualization.datasource.QueryPair;
import com.google.visualization.datasource.base.DataSourceException;
import com.google.visualization.datasource.datatable.DataTable;
import com.google.visualization.datasource.datatable.TableCell;
import com.google.visualization.datasource.datatable.TableRow;
import com.google.visualization.datasource.datatable.value.*;
import com.google.visualization.datasource.render.EscapeUtil;
import com.ibm.icu.util.GregorianCalendar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.Resource;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.google.visualization.datasource.impls.AbstractDataTableGenerator;
import org.systemsbiology.google.visualization.datasource.impls.CsvFileDataTableGenerator;
import org.systemsbiology.google.visualization.datasource.impls.TsvFileDataTableGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import java.util.logging.Logger;

import static com.google.visualization.datasource.DataSourceHelper.applyQuery;
import static com.google.visualization.datasource.DataSourceHelper.splitQuery;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang.StringUtils.*;

/**
 * @author hrovira
 */
public class DataSourceHelper {
    private static final Logger log = Logger.getLogger(DataSourceHelper.class.getName());


    public static void queryResource(HttpServletRequest request, HttpServletResponse response, Resource resource) throws Exception {
        InputStream checkStream = resource.getInputStream();
        InputStream inputStream = resource.getInputStream();
        try {
            AbstractDataTableGenerator dataTableGenerator = getDataTableGeneratorByOutputType(checkStream, inputStream);
            if (dataTableGenerator == null) {
                throw new InvalidSyntaxException("file cannot be queried");
            }

            executeDataSourceServletFlow(request, response, dataTableGenerator);
        } catch (Exception e) {
            log.warning(request.getRequestURI() + ":" + e);
        } finally {
            checkStream.close();
            inputStream.close();
        }
    }

    public static void executeDataSourceServletFlow(HttpServletRequest request, HttpServletResponse response,
                                                    DataTableGenerator tableGenerator) throws Exception {
        boolean isJsonArray = false;
        boolean isTsvOut = false;
        boolean isCsvPlain = false;
        String tqx = request.getParameter("tqx");
        if (!isEmpty(tqx)) {
            TqxParser tqxParser = new TqxParser(tqx);
            isJsonArray = equalsIgnoreCase(tqxParser.getOut(), "json_array");
            isTsvOut = equalsIgnoreCase(tqxParser.getOut(), "tsv");
            isCsvPlain = equalsIgnoreCase(tqxParser.getOut(), "csv_plain");
        }

        if (isJsonArray) {
            executeJsonArrayDataSourceServletFlow(request, response, tableGenerator);
        } else if (isTsvOut) {
            executeSimpleOutDataSourceServletFlow(request, response, tableGenerator, "\t");
        } else if (isCsvPlain) {
            executeSimpleOutDataSourceServletFlow(request, response, tableGenerator, ",");
        } else {
            com.google.visualization.datasource.DataSourceHelper.executeDataSourceServletFlow(request, response, tableGenerator, false);
        }
    }

    public static void executeJsonArrayDataSourceServletFlow(HttpServletRequest req, HttpServletResponse resp, DataTableGenerator dtGenerator) throws Exception {
        try {
            DataSourceRequest dsRequest = new DataSourceRequest(req);
            QueryPair query = splitQuery(dsRequest.getQuery(), dtGenerator.getCapabilities());
            DataTable dataTable = dtGenerator.generateDataTable(query.getDataSourceQuery(), req);
            DataTable newDataTable = applyQuery(query.getCompletionQuery(), dataTable, dsRequest.getUserLocale());

            JSONArray responseJson = generateResponse(newDataTable);

            resp.setContentType("application/json");
            resp.getWriter().write(responseJson.toString());

        } catch (DataSourceException e) {
            resp.setStatus(SC_BAD_REQUEST);
            resp.getWriter().write(e.getMessageToUser());

        } catch (RuntimeException e) {
            resp.setStatus(SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(e.getMessage());
        }
    }

    public static void executeSimpleOutDataSourceServletFlow(HttpServletRequest req, HttpServletResponse resp, DataTableGenerator dtGenerator, String separator) throws Exception {
        try {
            DataSourceRequest dsRequest = new DataSourceRequest(req);
            QueryPair query = splitQuery(dsRequest.getQuery(), dtGenerator.getCapabilities());
            DataTable dataTable = dtGenerator.generateDataTable(query.getDataSourceQuery(), req);
            DataTable newDataTable = applyQuery(query.getCompletionQuery(), dataTable, dsRequest.getUserLocale());

            resp.setContentType("text/plain");
            generateResponse(newDataTable, separator, resp);

        } catch (DataSourceException e) {
            resp.setStatus(SC_BAD_REQUEST);
            resp.getWriter().write(e.getMessageToUser());

        } catch (RuntimeException e) {
            resp.setStatus(SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(e.getMessage());
        }
    }

    public static AbstractDataTableGenerator getDataTableGeneratorByOutputType(InputStream checkStream, InputStream inputStream) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(checkStream));
            String columnHeader = reader.readLine();
            if (!isEmpty(columnHeader)) {
                if (contains(columnHeader, "\t")) {
                    return new TsvFileDataTableGenerator(inputStream, columnHeader);
                }
                if (contains(columnHeader, ",")) {
                    return new CsvFileDataTableGenerator(inputStream, columnHeader);
                }
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                log.warning(e.getMessage());
            }
        }
        return null;
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
            if (hasValidCells(cells)) {
                for (int i = 0; i < cells.size(); i++) {
                    jsonRow.put(data.getColumnDescription(i).getLabel(), getCellValue(cells.get(i)));
                }
                jsonArray.put(jsonRow);
            }
        }
        return jsonArray;
    }

    private static void generateResponse(DataTable data, String separator, HttpServletResponse response) throws Exception {
        if (!data.getWarnings().isEmpty()) {
            return;
        }

        if (data.getColumnDescriptions().isEmpty()) {
            return;
        }

        PrintWriter writer = response.getWriter();
        boolean firstRow = true;
        for (TableRow tableRow : data.getRows()) {
            List<TableCell> cells = tableRow.getCells();
            if (hasValidCells(cells)) {
                if (firstRow) {
                    for (int i = 0; i < cells.size(); i++) {
                        if (i != 0) {
                            writer.print(separator);
                        }
                        writer.print(data.getColumnDescription(i).getLabel());
                    }
                    writer.println();

                    firstRow = false;
                }

                for (int i = 0; i < cells.size(); i++) {
                    if (i != 0) {
                        writer.print(separator);
                    }
                    writer.print(getCellValue(cells.get(i)).toString());
                }
                writer.println();
            }
        }
    }

    private static boolean hasValidCells(List<TableCell> cells) throws JSONException {
        for (TableCell cell : cells) {
            Value value = cell.getValue();
            if (value != null && !value.isNull()) {
                return true;
            }
        }
        return false;
    }

    private static Object getCellValue(TableCell cell) {
        Value value = cell.getValue();
        ValueType type = cell.getType();

        if ((value == null) || (value.isNull())) {
            return "";
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
