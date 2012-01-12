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

import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.chromosome.index.callbacks.MinMaxRangeResultSetExtractor;
import org.systemsbiology.addama.chromosome.index.callbacks.RangeQueryResultSetExtractor;
import org.systemsbiology.addama.chromosome.index.pojos.QueryParams;
import org.systemsbiology.addama.chromosome.index.pojos.Schema;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.jsonconfig.impls.JsonPropertyByIdMappingsHandler;
import org.systemsbiology.google.visualization.datasource.jdbc.DatabaseTableColumnConnectionCallback;
import org.systemsbiology.google.visualization.datasource.jdbc.JdbcTemplateMappingsHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static org.systemsbiology.addama.commons.web.utils.HttpIO.getURI;

/**
 * @author hrovira
 */
@Controller
public class ChromIndexServiceController {
    private final Map<String, JdbcTemplate> jdbcTemplateById = new HashMap<String, JdbcTemplate>();
    private final Map<String, JSONObject> schemasById = new HashMap<String, JSONObject>();
    private Iterable<Mapping> mappings;

    public void setServiceConfig(ServiceConfig serviceConfig) throws Exception {
        mappings = serviceConfig.getMappings();

        serviceConfig.visit(new JdbcTemplateMappingsHandler(jdbcTemplateById));
        serviceConfig.visit(new JsonPropertyByIdMappingsHandler(schemasById, "schema"));
    }

    @RequestMapping(value = "/**/indexes", method = RequestMethod.GET)
    protected ModelAndView builds(HttpServletRequest request) throws Exception {
        String uri = getURI(request);

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

    @RequestMapping(value = "/**/indexes/{build}", method = RequestMethod.GET)
    protected ModelAndView build(HttpServletRequest request, @PathVariable("build") String build) throws Exception {
        String uri = getURI(request);

        JdbcTemplate jdbcTemplate = jdbcTemplateById.get(build);
        JSONObject schema = schemasById.get(build);
        if (jdbcTemplate == null || schema == null) {
            throw new ResourceNotFoundException(uri);
        }

        String tableName = schema.getString("table");

        JSONObject json = jdbcTemplate.execute(new DatabaseTableColumnConnectionCallback(tableName, "dataSchema"));
        json.put("uri", uri);

        String sql = "SELECT DISTICT " + schema.optString("chromosome", "chrom") + " FROM " + tableName;
        for (String chromosome : jdbcTemplate.queryForList(sql, String.class)) {
            JSONObject item = new JSONObject();
            item.put("id", chromosome);
            item.put("label", chromosome);
            item.put("uri", uri + "/" + chromosome);
            json.append("items", item);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/indexes/{build}/{chromosome}", method = RequestMethod.GET)
    protected ModelAndView chromosome(HttpServletRequest request,
                                      @PathVariable("build") String build,
                                      @PathVariable("chromosome") String chromosome) throws Exception {
        String uri = getURI(request);

        JdbcTemplate jdbcTemplate = jdbcTemplateById.get(build);
        JSONObject schema = schemasById.get(build);
        if (jdbcTemplate == null || schema == null) {
            throw new ResourceNotFoundException(uri);
        }

        final JSONObject json = new JSONObject();
        json.put("uri", uri);

        MinMaxRangeResultSetExtractor extractor = new MinMaxRangeResultSetExtractor(schema);
        json.put("range", jdbcTemplate.query(extractor.SQL(), new Object[]{chromosome}, extractor));
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/indexes/{build}/{chromosome}/{start}/{end}", method = RequestMethod.GET)
    protected ModelAndView range_query(HttpServletRequest request,
                                       @PathVariable("build") String build,
                                       @PathVariable("chromosome") String chromosome,
                                       @PathVariable("start") Long start,
                                       @PathVariable("end") Long end) throws Exception {
        String uri = getURI(request);
        JdbcTemplate jdbcTemplate = jdbcTemplateById.get(build);
        JSONObject schema = schemasById.get(build);
        if (jdbcTemplate == null || schema == null) {
            throw new ResourceNotFoundException("");
        }

        JSONObject json = new JSONObject();
        json.put("uri", uri);

        QueryParams qp = new QueryParams(build, chromosome, start, end);
        RangeQueryResultSetExtractor extractor = new RangeQueryResultSetExtractor(new Schema(schema), qp);
        for (JSONObject item : jdbcTemplate.query(extractor.PS(), extractor.ARGS(), extractor)) {
            json.append("items", item);
        }
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/indexes/{build}/{chromosome}/{start}/{end}/{strand}", method = RequestMethod.GET)
    protected ModelAndView range_strand(HttpServletRequest request, @PathVariable("build") String build,
                                        @PathVariable("chromosome") String chromosome,
                                        @PathVariable("start") Long start,
                                        @PathVariable("end") Long end,
                                        @PathVariable("strand") String strand) throws Exception {
        String uri = getURI(request);
        JdbcTemplate jdbcTemplate = jdbcTemplateById.get(build);
        JSONObject schema = schemasById.get(build);
        if (jdbcTemplate == null || schema == null) {
            throw new ResourceNotFoundException("");
        }

        JSONObject json = new JSONObject();
        json.put("uri", uri);

        QueryParams qp = new QueryParams(build, chromosome, start, end, strand);
        RangeQueryResultSetExtractor extractor = new RangeQueryResultSetExtractor(new Schema(schema), qp);
        for (JSONObject item : jdbcTemplate.query(extractor.PS(), extractor.ARGS(), extractor)) {
            json.append("items", item);
        }
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

}
