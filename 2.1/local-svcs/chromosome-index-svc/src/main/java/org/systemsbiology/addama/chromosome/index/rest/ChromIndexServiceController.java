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

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.systemsbiology.addama.chromosome.index.callbacks.ChromUriResultSetExtractor;
import org.systemsbiology.addama.chromosome.index.callbacks.GeneChromUriResultSetExtractor;
import org.systemsbiology.addama.chromosome.index.callbacks.ItemedResultSetExtractor;
import org.systemsbiology.addama.chromosome.index.callbacks.LocusUriResultSetExtractor;
import org.systemsbiology.addama.chromosome.index.pojos.ChromUriBean;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.jsonconfig.impls.JSONObjectMapJsonConfigHandler;
import org.systemsbiology.google.visualization.datasource.jdbc.JdbcTemplateJsonConfigHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class ChromIndexServiceController extends AbstractController {
    private static final Logger log = Logger.getLogger(ChromIndexServiceController.class.getName());

    private final Map<String, JdbcTemplate> jdbcTemplateByUri = new HashMap<String, JdbcTemplate>();
    private final Map<String, JSONObject> genesSchemasByUri = new HashMap<String, JSONObject>();
    private final Map<String, JSONObject> locusSchemasByUri = new HashMap<String, JSONObject>();

    public void setJsonConfig(JsonConfig jsonConfig) {
        jsonConfig.visit(new JdbcTemplateJsonConfigHandler(jdbcTemplateByUri));
        jsonConfig.visit(new JSONObjectMapJsonConfigHandler(genesSchemasByUri, "genesSchema"));
        jsonConfig.visit(new JSONObjectMapJsonConfigHandler(locusSchemasByUri, "locusSchema"));
    }

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = request.getRequestURI();
        log.info(requestUri);

        try {
            ChromUriBean bean = new ChromUriBean(request, "/addama/refgenome");
            String datasourceUri = "/addama/refgenome/" + bean.getBuild() + "/genes";

            JdbcTemplate jdbcTemplate = jdbcTemplateByUri.get(datasourceUri);
            if (jdbcTemplate == null) {
                throw new ResourceNotFoundException(datasourceUri);
            }

            ItemedResultSetExtractor extractor = getResultSetExtractor(requestUri, datasourceUri, bean);
            jdbcTemplate.query(extractor.getPreparedStatement(), extractor.getArguments(), extractor);
            return new ModelAndView(new JsonItemsView()).addObject("json", extractor.getItems());
        } catch (NumberFormatException e) {
            log.warning("not a chrom range uri, check locus: " + e);

            if (requestUri.endsWith("/locus")) {
                String build = StringUtils.substringBetween(requestUri, "/addama/refgenome/", "/genes");
                String datasourceUri = "/addama/refgenome/" + build + "/genes";
                String geneUri = StringUtils.substringBetween(requestUri, request.getContextPath(), "/locus");
                log.info("geneUri=" + geneUri);

                ChromUriBean[] beans = getChromUris(datasourceUri, geneUri);
                JSONObject[] locusItems = getLocusItems(datasourceUri, beans);
                log.info("locusItems=" + locusItems.length);

                JSONObject json = new JSONObject();
                for (JSONObject locusItem : locusItems) {
                    json.append("items", locusItem);
                }

                return new ModelAndView(new JsonItemsView()).addObject("json", json);
            }
            throw new ResourceNotFoundException(requestUri);
        }
    }

    /*
     * Private Methods
     */

    private ItemedResultSetExtractor getResultSetExtractor(String requestUri, String datasourceUri, ChromUriBean bean) throws Exception {
        if (requestUri.endsWith("/locus")) {
            JSONObject schema = locusSchemasByUri.get(datasourceUri);
            if (schema == null) {
                throw new ResourceNotFoundException(datasourceUri);
            }
            return new LocusUriResultSetExtractor(schema, bean);
        }

        JSONObject schema = genesSchemasByUri.get(datasourceUri);
        if (schema == null) {
            throw new ResourceNotFoundException(datasourceUri);
        }
        return new ChromUriResultSetExtractor(schema, bean);
    }

    private ChromUriBean[] getChromUris(String datasourceUri, String geneUri) throws Exception {
        JdbcTemplate jdbcTemplate = jdbcTemplateByUri.get(datasourceUri);
        JSONObject genesSchema = genesSchemasByUri.get(datasourceUri);
        GeneChromUriResultSetExtractor rse = new GeneChromUriResultSetExtractor(genesSchema, geneUri);
        ChromUriBean[] beans = (ChromUriBean[]) jdbcTemplate.query(rse.getPreparedStatement(), rse.getArguments(), rse);
        if (beans == null) {
            return new ChromUriBean[0];
        }
        return beans;
    }

    private JSONObject[] getLocusItems(String datasourceUri, ChromUriBean[] beans) throws Exception {
        JdbcTemplate jdbcTemplate = jdbcTemplateByUri.get(datasourceUri);
        JSONObject locusSchema = locusSchemasByUri.get(datasourceUri);

        HashMap<String, JSONObject> map = new HashMap<String, JSONObject>();
        for (ChromUriBean bean : beans) {
            ItemedResultSetExtractor extractor = new LocusUriResultSetExtractor(locusSchema, bean);
            jdbcTemplate.query(extractor.getPreparedStatement(), extractor.getArguments(), extractor);
            JSONObject json = extractor.getItems();
            log.info("items=" + json.toString());
            if (json.has("items")) {
                JSONArray jsonitems = json.getJSONArray("items");
                for (int i = 0; i < jsonitems.length(); i++) {
                    JSONObject jsonitem = jsonitems.getJSONObject(i);
                    map.put(jsonitem.getString("uri"), jsonitem);
                }
            }
        }

        return map.values().toArray(new JSONObject[map.size()]);
    }

}
