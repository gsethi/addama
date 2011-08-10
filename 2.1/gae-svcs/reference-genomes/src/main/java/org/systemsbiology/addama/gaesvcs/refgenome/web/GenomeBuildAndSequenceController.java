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
package org.systemsbiology.addama.gaesvcs.refgenome.web;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.gaesvcs.refgenome.ReferenceGenomeServer;
import org.systemsbiology.addama.gaesvcs.refgenome.pojos.ChromUriBean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class GenomeBuildAndSequenceController extends AbstractController {
    private static final Logger log = Logger.getLogger(GenomeBuildAndSequenceController.class.getName());

    private Map<String, ReferenceGenomeServer> referenceGenomeServersByBuild;

    public void setReferenceGenomeServersByBuild(Map<String, ReferenceGenomeServer> map) {
        this.referenceGenomeServersByBuild = map;
    }

    /*
     * AbstractController
     */

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ChromUriBean bean = new ChromUriBean(request);
        log.info("handleRequestInternal(" + request.getRequestURI() + "):" + bean);

        if (bean.getUri().endsWith("/sequence")) {
            getSequencesAtPosition(bean, request, response);
            return null;
        }

        if (bean.getBuild() == null) {
            return getBuilds(bean);
        }

        if (bean.getChromosome() == null) {
            return getChromosomes(bean);
        }

        if (bean.getStart() == null) {
            return getChromosome(bean);
        }

        throw new ResourceNotFoundException(bean.getUri());
    }

    /*
     * Private Methods
     */

    private ModelAndView getBuilds(ChromUriBean bean) throws Exception {
        log.info("getBuilds(" + bean + ")");

        JSONObject json = new JSONObject();
        for (String build : referenceGenomeServersByBuild.keySet()) {
            JSONObject buildjson = new JSONObject();
            buildjson.put("name", build);
            buildjson.put("uri", bean.getUri() + "/" + build);
            json.append("items", buildjson);
        }

        ModelAndView mav = new ModelAndView(new JsonItemsView());
        mav.addObject("json", json);
        return mav;
    }

    private ModelAndView getChromosomes(ChromUriBean bean) throws Exception {
        log.info("getChromosomes(" + bean + ")");

        ReferenceGenomeServer server = getReferenceGenomeServer(bean);
        String[] chromosomes = server.getChromosomes();
        if (chromosomes == null) {
            throw new ResourceNotFoundException(bean.getUri());
        }

        JSONObject json = new JSONObject();
        for (String chromosome : chromosomes) {
            JSONObject chrjson = new JSONObject();
            chrjson.put("name", chromosome);
            chrjson.put("uri", bean.getUri() + "/" + chromosome);
            json.append("items", chrjson);
        }

        ModelAndView mav = new ModelAndView(new JsonItemsView());
        mav.addObject("json", json);
        return mav;
    }

    private ModelAndView getChromosome(ChromUriBean bean) throws Exception {
        log.info("getChromosome(" + bean + ")");


        JSONObject json = new JSONObject();
        json.put("name", bean.getChromosome());
        json.put("uri", bean.getUri());

        ReferenceGenomeServer server = getReferenceGenomeServer(bean);
        Long length = server.getChromosomeLength(bean.getChromosome());
        if (length != null) {
            json.put("length", length);
        } else {
            json.put("length", "NaN");
        }

        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", json);
        return mav;
    }

    private void getSequencesAtPosition(ChromUriBean bean, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("getSequencesAtPosition(" + bean + "):" + request.getMethod());

        ReferenceGenomeServer server = getReferenceGenomeServer(bean);

        String method = request.getMethod();
        if (StringUtils.equalsIgnoreCase(method, "HEAD")) {
            if (bean.getEnd() != null && bean.getStart() != null) {
                Long length = bean.getEnd() - bean.getStart();
                log.info("length=" + length);
                if (length != null) {
                    response.addHeader("x-addama-content-length", length.toString());
                }
            }
            return;
        }

        String outputFilename = request.getParameter("outputFilename");
        if (!StringUtils.isEmpty(outputFilename)) {
            response.setHeader("Content-Disposition", "filename=\"" + outputFilename + "\"");
            response.setContentType(super.getServletContext().getMimeType(outputFilename));
        }

        server.loadSequence(response.getOutputStream(), bean.getChromosome(), bean.getStart(), bean.getEnd());
    }

    private ReferenceGenomeServer getReferenceGenomeServer(ChromUriBean bean) throws ResourceNotFoundException {
        ReferenceGenomeServer server = referenceGenomeServersByBuild.get(bean.getBuild());
        if (server == null) {
            throw new ResourceNotFoundException(bean.getUri());
        }
        return server;
    }
}
