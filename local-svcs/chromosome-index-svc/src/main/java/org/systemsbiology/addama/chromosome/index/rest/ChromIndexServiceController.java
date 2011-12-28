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
import org.systemsbiology.addama.chromosome.index.callbacks.ChromUriResultSetExtractor;
import org.systemsbiology.addama.chromosome.index.callbacks.ItemedResultSetExtractor;
import org.systemsbiology.addama.chromosome.index.callbacks.LocusUriResultSetExtractor;
import org.systemsbiology.addama.chromosome.index.pojos.ChromUriBean;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.jsonconfig.impls.JsonPropertyByIdMappingsHandler;
import org.systemsbiology.google.visualization.datasource.jdbc.JdbcTemplateMappingsHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hrovira
 */
@Controller
public class ChromIndexServiceController {
    private final Map<String, JdbcTemplate> jdbcTemplateById = new HashMap<String, JdbcTemplate>();
    private final Map<String, JSONObject> genesSchemasById = new HashMap<String, JSONObject>();
    private final Map<String, JSONObject> locusSchemasById = new HashMap<String, JSONObject>();

    public void setServiceConfig(ServiceConfig serviceConfig) throws Exception {
        serviceConfig.visit(new JdbcTemplateMappingsHandler(jdbcTemplateById));
        serviceConfig.visit(new JsonPropertyByIdMappingsHandler(genesSchemasById, "genesSchema"));
        serviceConfig.visit(new JsonPropertyByIdMappingsHandler(locusSchemasById, "locusSchema"));
    }

    @RequestMapping(value = "/**/refgenome/{build}/{chromosome}/{start}/{end}/{strand}/genes", method = RequestMethod.GET)
    protected ModelAndView queryGenes(@PathVariable("build") String build,
                                      @PathVariable("chromosome") String chromosome,
                                      @PathVariable("start") Long start,
                                      @PathVariable("end") Long end,
                                      @PathVariable("strand") String strand) throws Exception {
        JdbcTemplate jdbcTemplate = jdbcTemplateById.get(build);
        JSONObject schema = genesSchemasById.get(build);
        if (jdbcTemplate == null || schema == null) {
            throw new ResourceNotFoundException("");
        }

        ChromUriBean bean = new ChromUriBean(build, chromosome, start, end, strand);
        ItemedResultSetExtractor extractor = new ChromUriResultSetExtractor(schema, bean);
        jdbcTemplate.query(extractor.getPreparedStatement(), extractor.getArguments(), extractor);
        return new ModelAndView(new JsonItemsView()).addObject("json", extractor.getItems());
    }

    @RequestMapping(value = "/**/refgenome/{build}/{chromosome}/{start}/{end}/locus", method = RequestMethod.GET)
    protected ModelAndView queryLocus(@PathVariable("build") String build,
                                      @PathVariable("chromosome") String chromosome,
                                      @PathVariable("start") Long start,
                                      @PathVariable("end") Long end,
                                      @PathVariable("strand") String strand) throws Exception {
        JdbcTemplate jdbcTemplate = jdbcTemplateById.get(build);
        JSONObject schema = locusSchemasById.get(build);
        if (jdbcTemplate == null || schema == null) {
            throw new ResourceNotFoundException(build);
        }

        ChromUriBean bean = new ChromUriBean(build, chromosome, start, end, strand);
        ItemedResultSetExtractor extractor = new LocusUriResultSetExtractor(schema, bean);
        jdbcTemplate.query(extractor.getPreparedStatement(), extractor.getArguments(), extractor);
        return new ModelAndView(new JsonItemsView()).addObject("json", extractor.getItems());
    }

}
