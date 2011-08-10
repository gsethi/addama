package org.systemsbiology.addama.services.fs.datasources.rest;

import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.impl.DefaultFileMonitor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.filedbcreate.TableBean;
import org.systemsbiology.addama.filedbcreate.callbacks.TableDDLJsonConfig;
import org.systemsbiology.addama.filedbcreate.etl.TableDDL;
import org.systemsbiology.addama.filedbcreate.jdbc.ColumnBeansJsonResultSetExtractor;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.jsonconfig.impls.IntegerMapJsonConfigHandler;
import org.systemsbiology.addama.jsonconfig.impls.StringMapJsonConfigHandler;
import org.systemsbiology.addama.services.fs.datasources.jdbc.TablesJsonResultSetExtractor;
import org.systemsbiology.addama.services.fs.datasources.vfs.DbLoadDataFileListener;
import org.systemsbiology.google.visualization.datasource.jdbc.JdbcTemplateDataTableGenerator;
import org.systemsbiology.google.visualization.datasource.mvc.QueryController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.vfs.VFS.getManager;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.getCleanUri;
import static org.systemsbiology.addama.filedbcreate.dao.UriTableMappings.*;
import static org.systemsbiology.addama.services.fs.datasources.jdbc.TableLookupResultSetExtractor.findTables;
import static org.systemsbiology.google.visualization.datasource.DataSourceHelper.executeDataSourceServletFlow;

/**
 * @author hrovira
 */
@Controller
public class DatasourcesController extends QueryController implements InitializingBean {
    private static final Logger log = Logger.getLogger(DatasourcesController.class.getName());

    private final Map<String, String> rootPathsByUri = new HashMap<String, String>();
    private final Map<String, TableDDL> tableDDLsByUri = new HashMap<String, TableDDL>();
    private final Map<String, Integer> maxIndexesByUri = new HashMap<String, Integer>();

    @Override
    public void setJsonConfig(JsonConfig jsonConfig) {
        super.setJsonConfig(jsonConfig);
        jsonConfig.visit(new StringMapJsonConfigHandler(rootPathsByUri, "rootPath"));
        jsonConfig.visit(new IntegerMapJsonConfigHandler(maxIndexesByUri, "maxIndexes"));
        jsonConfig.visit(new TableDDLJsonConfig(tableDDLsByUri, "schemaType"));
    }

    public void afterPropertiesSet() throws Exception {
        if (rootPathsByUri.isEmpty()) {
            log.info("no datasources configured");
            return;
        }

        FileSystemManager manager = getManager();

        for (Map.Entry<String, String> entry : rootPathsByUri.entrySet()) {
            String uri = entry.getKey();
            String rootPath = entry.getValue();

            JdbcTemplate jdbcTemplate = getJdbcTemplate(uri);
            createTable(jdbcTemplate);

            DbLoadDataFileListener fileListener = new DbLoadDataFileListener(rootPath, jdbcTemplate);
            if (tableDDLsByUri.containsKey(uri)) fileListener.setTableDDL(tableDDLsByUri.get(uri));
            if (maxIndexesByUri.containsKey(uri)) fileListener.setMaxIndexes(maxIndexesByUri.get(uri));

            DefaultFileMonitor fileMon = new DefaultFileMonitor(fileListener);
            fileMon.setRecursive(true);
            fileMon.addFile(manager.resolveFile(rootPath));
            fileMon.start();
        }
    }

    @RequestMapping(value = "/**/query", method = RequestMethod.GET)
    public void query(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = getCleanUri(request, "/query");
        JdbcTemplate jdbcTemplate = getJdbcTemplate(requestUri);

        TableBean[] tables = findTables(requestUri, jdbcTemplate);
        if (tables.length == 0) {
            throw new ResourceNotFoundException(requestUri);
        }
        if (tables.length > 1) {
            log.warning("found more than one matching table: querying first one:" + requestUri);
        }

        String tableName = tables[0].getTableName();
        executeDataSourceServletFlow(request, response, new JdbcTemplateDataTableGenerator(jdbcTemplate, tableName));
    }

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView get(HttpServletRequest request) throws Exception {
        String requestUri = getCleanUri(request);
        JdbcTemplate jdbcTemplate = getJdbcTemplate(requestUri);

        TableBean[] tables = findTables(requestUri, jdbcTemplate);
        if (tables.length == 1) {
            Object json = columnsByTableUri(jdbcTemplate, tables[0], new ColumnBeansJsonResultSetExtractor());
            return new ModelAndView(new JsonItemsView()).addObject("json", json);
        }

        Object json = matchByUri(jdbcTemplate, requestUri, new TablesJsonResultSetExtractor());
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }
}
