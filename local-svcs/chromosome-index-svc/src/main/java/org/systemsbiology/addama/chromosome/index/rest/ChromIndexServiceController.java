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
package org.systemsbiology.addama.chromosome.index.rest;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.chromosome.index.callbacks.MinMaxRangeResultSetExtractor;
import org.systemsbiology.addama.chromosome.index.callbacks.RangeQueryDistinctGenesResultSetExtractor;
import org.systemsbiology.addama.chromosome.index.callbacks.RangeQueryResultSetExtractor;
import org.systemsbiology.addama.chromosome.index.callbacks.SchemaByIdMappingsHandler;
import org.systemsbiology.addama.chromosome.index.pojos.QueryParams;
import org.systemsbiology.addama.chromosome.index.pojos.Schema;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.google.visualization.datasource.jdbc.DatabaseTableColumnConnectionCallback;
import org.systemsbiology.google.visualization.datasource.jdbc.JdbcTemplateMappingsHandler;
import org.systemsbiology.google.visualization.datasource.jdbc.SingleStringResultSetExtractor;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.getURI;

/**
 * @author hrovira
 */
@Controller
public class ChromIndexServiceController {
    private static final Logger log = Logger.getLogger(ChromIndexServiceController.class.getName());

    private final Map<String, JdbcTemplate> jdbcTemplateById = new HashMap<String, JdbcTemplate>();
    private final Map<String, Schema> schemasById = new HashMap<String, Schema>();
    private Iterable<Mapping> mappings;

    public void setServiceConfig(ServiceConfig serviceConfig) throws Exception {
        mappings = serviceConfig.getMappings();

        serviceConfig.visit(new JdbcTemplateMappingsHandler(jdbcTemplateById));
        serviceConfig.visit(new SchemaByIdMappingsHandler(schemasById));
    }

    @RequestMapping(value = "/**/chromosomes", method = GET)
    protected ModelAndView builds(HttpServletRequest request) throws Exception {
        String uri = getURI(request);
        log.info(uri);

        JSONObject json = new JSONObject();
        json.put("uri", uri);
        for (Mapping mapping : mappings) {
            JSONObject item = new JSONObject();
            item.put("id", mapping.ID());
            item.put("uri", uri + "/" + mapping.ID());
            item.put("label", mapping.LABEL());
            json.append("items", item);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/chromosomes/{build}", method = GET)
    protected ModelAndView build(HttpServletRequest request, @PathVariable("build") String build) throws Exception {
        String uri = getURI(request);
        log.info(uri);

        JdbcTemplate jdbcTemplate = getJdbcTemplate(build);
        Schema schema = getSchema(build);

        String tableName = schema.getTableName();

        JSONObject json = jdbcTemplate.execute(new DatabaseTableColumnConnectionCallback(tableName, "dataSchema"));
        json.put("uri", uri);
        json.put("genes", uri + "/genes");

        String sql = "SELECT DISTINCT " + schema.getChromosomeColumn() + " FROM " + tableName;
        for (String chromosome : jdbcTemplate.queryForList(sql, String.class)) {
            JSONObject item = new JSONObject();
            item.put("id", chromosome);
            item.put("label", chromosome);
            item.put("uri", uri + "/" + chromosome);
            json.append("items", item);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    /*
     * Gene Lookup Methods
     */

    @RequestMapping(value = "/**/chromosomes/{build}/genes", method = GET)
    protected ModelAndView genes_build(HttpServletRequest request, @PathVariable("build") String build) throws Exception {
        String uri = getURI(request);
        log.info(uri);

        JdbcTemplate jdbcTemplate = getJdbcTemplate(build);
        Schema schema = getSchema(build);

        String sql = "SELECT DISTINCT " + schema.getGeneIdentifierColumn() + " FROM " + schema.getTableName();

        JSONObject json = new JSONObject();
        json.put("dataSchema", new JSONObject().put("name", schema.getGeneIdentifierColumn()).put("datatype", "string"));
        json.put("data", jdbcTemplate.query(sql, new SingleStringResultSetExtractor()));
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/chromosomes/{build}/genes/{chromosome}", method = GET)
    protected ModelAndView genes_chromosome(HttpServletRequest request,
                                            @PathVariable("build") String build,
                                            @PathVariable("chromosome") String chromosome) throws Exception {
        String uri = getURI(request);
        log.info(uri);

        JdbcTemplate jdbcTemplate = getJdbcTemplate(build);
        Schema schema = getSchema(build);

        QueryParams qp = new QueryParams(build, chromosome);
        RangeQueryDistinctGenesResultSetExtractor extractor = new RangeQueryDistinctGenesResultSetExtractor(schema, qp);

        JSONObject json = new JSONObject();
        json.put("dataSchema", new JSONObject().put("name", schema.getGeneIdentifierColumn()).put("datatype", "string"));
        json.put("data", jdbcTemplate.query(extractor.PS(), extractor.ARGS(), extractor));
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/chromosomes/{build}/genes/{chromosome}/{start}/{end}", method = GET)
    protected ModelAndView genes_range_query(HttpServletRequest request,
                                             @PathVariable("build") String build,
                                             @PathVariable("chromosome") String chromosome,
                                             @PathVariable("start") Long start,
                                             @PathVariable("end") Long end) throws Exception {
        String uri = getURI(request);
        log.info(uri);

        JdbcTemplate jdbcTemplate = getJdbcTemplate(build);
        Schema schema = getSchema(build);

        QueryParams qp = new QueryParams(build, chromosome, start, end);
        RangeQueryDistinctGenesResultSetExtractor extractor = new RangeQueryDistinctGenesResultSetExtractor(schema, qp);

        JSONObject json = new JSONObject();
        json.put("dataSchema", new JSONObject().put("name", schema.getGeneIdentifierColumn()).put("datatype", "string"));
        json.put("data", jdbcTemplate.query(extractor.PS(), extractor.ARGS(), extractor));
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/chromosomes/{build}/genes/{chromosome}/{start}/{end}/{strand}", method = GET)
    protected ModelAndView genes_range_strand(HttpServletRequest request, @PathVariable("build") String build,
                                              @PathVariable("chromosome") String chromosome,
                                              @PathVariable("start") Long start,
                                              @PathVariable("end") Long end,
                                              @PathVariable("strand") String strand) throws Exception {
        String uri = getURI(request);
        log.info(uri);

        JdbcTemplate jdbcTemplate = getJdbcTemplate(build);
        Schema schema = getSchema(build);

        QueryParams qp = new QueryParams(build, chromosome, start, end, strand);
        RangeQueryDistinctGenesResultSetExtractor extractor = new RangeQueryDistinctGenesResultSetExtractor(schema, qp);

        JSONObject json = new JSONObject();
        json.put("dataSchema", new JSONObject().put("name", schema.getGeneIdentifierColumn()).put("datatype", "string"));
        json.put("data", jdbcTemplate.query(extractor.PS(), extractor.ARGS(), extractor));
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    /*
    * Chromosome Query Methods
    */

    @RequestMapping(value = "/**/chromosomes/{build}/{chromosome}", method = GET)
    protected ModelAndView chromosome(HttpServletRequest request,
                                      @PathVariable("build") String build,
                                      @PathVariable("chromosome") String chromosome) throws Exception {
        String uri = getURI(request);
        log.info(uri);

        JdbcTemplate jdbcTemplate = getJdbcTemplate(build);
        Schema schema = getSchema(build);

        MinMaxRangeResultSetExtractor extractor = new MinMaxRangeResultSetExtractor(schema);
        JSONObject json = jdbcTemplate.query(extractor.SQL(), new Object[]{chromosome}, extractor);
        json.put("uri", uri);
        json.put("chromosome", chromosome);

        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/chromosomes/{build}/{chromosome}/{start}/{end}", method = GET)
    protected ModelAndView range_query(HttpServletRequest request,
                                       @PathVariable("build") String build,
                                       @PathVariable("chromosome") String chromosome,
                                       @PathVariable("start") Long start,
                                       @PathVariable("end") Long end) throws Exception {
        String uri = getURI(request);
        log.info(uri);

        JdbcTemplate jdbcTemplate = getJdbcTemplate(build);
        Schema schema = getSchema(build);

        JSONObject json = new JSONObject();

        QueryParams qp = new QueryParams(build, chromosome, start, end);
        RangeQueryResultSetExtractor extractor = new RangeQueryResultSetExtractor(schema, qp);
        for (JSONObject item : jdbcTemplate.query(extractor.PS(), extractor.ARGS(), extractor)) {
            json.append("items", item);
        }
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/chromosomes/{build}/{chromosome}/{start}/{end}/{strand}", method = GET)
    protected ModelAndView range_strand(HttpServletRequest request, @PathVariable("build") String build,
                                        @PathVariable("chromosome") String chromosome,
                                        @PathVariable("start") Long start,
                                        @PathVariable("end") Long end,
                                        @PathVariable("strand") String strand) throws Exception {
        String uri = getURI(request);
        log.info(uri);

        JdbcTemplate jdbcTemplate = getJdbcTemplate(build);
        Schema schema = getSchema(build);

        JSONObject json = new JSONObject();

        QueryParams qp = new QueryParams(build, chromosome, start, end, strand);
        RangeQueryResultSetExtractor extractor = new RangeQueryResultSetExtractor(schema, qp);
        for (JSONObject item : jdbcTemplate.query(extractor.PS(), extractor.ARGS(), extractor)) {
            json.append("items", item);
        }
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    /*
     * Private Methods
     */

    private JdbcTemplate getJdbcTemplate(String build) throws ResourceNotFoundException {
        JdbcTemplate jdbcTemplate = jdbcTemplateById.get(build);
        if (jdbcTemplate == null) {
            throw new ResourceNotFoundException(build);
        }
        return jdbcTemplate;
    }

    private Schema getSchema(String build) throws ResourceNotFoundException, JSONException {
        Schema schema = schemasById.get(build);
        if (schema == null) {
            throw new ResourceNotFoundException(build);
        }
        return schema;
    }
}
