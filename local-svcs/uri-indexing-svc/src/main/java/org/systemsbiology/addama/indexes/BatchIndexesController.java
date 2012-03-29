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
package org.systemsbiology.addama.indexes;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.lucene.index.core.LuceneIndexTemplate;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.indexes.batches.BatchCallback;
import org.systemsbiology.addama.indexes.batches.BatchItem;
import org.systemsbiology.addama.indexes.batches.BatchSplitter;
import org.systemsbiology.addama.indexes.batches.ReferenceJsonBatchCallback;
import org.systemsbiology.addama.indexes.handlers.LuceneIndexTemplateMappingsHandler;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.getURI;

/**
 * @author hrovira
 */
@Controller
public class BatchIndexesController {
    private static final Logger log = Logger.getLogger(BatchIndexesController.class.getName());
    // TODO : Persist batches
    private static final Map<String, BatchItem> batchesByUri = new HashMap<String, BatchItem>();

    private final HashMap<String, LuceneIndexTemplate> indexTemplates = new HashMap<String, LuceneIndexTemplate>();

    public void setServiceConfig(ServiceConfig serviceConfig) throws Exception {
        serviceConfig.visit(new LuceneIndexTemplateMappingsHandler(indexTemplates));
    }

    @RequestMapping(value = "/**/indexes/{indexId}/batches", method = RequestMethod.POST)
    public ModelAndView batches(HttpServletRequest request, @PathVariable("indexId") String indexId) throws Exception {
        log.info(indexId);

        String baseUri = getURI(request);

        JSONObject json = executeBatch(indexId, baseUri, request);
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/batches", method = RequestMethod.GET)
    public ModelAndView getBatches() throws Exception {
        JSONObject json = new JSONObject();
        for (BatchItem batchItem : batchesByUri.values()) {
            json.append("items", getBatchJson(batchItem));
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "**/batches/*", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getBatch(HttpServletRequest request) throws Exception {
        String batchUri = getURI(request);

        BatchItem batch = batchesByUri.get(batchUri);
        if (batch == null) {
            throw new ResourceNotFoundException(batchUri);
        }

        JSONObject json = getBatchJson(batch);
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    /*
     * Private Methods
     */

    private LuceneIndexTemplate getLuceneIndexTemplate(String indexId) throws ResourceNotFoundException {
        LuceneIndexTemplate lit = indexTemplates.get(indexId);
        if (lit != null) {
            return lit;
        }
        throw new ResourceNotFoundException(indexId);
    }

    private JSONObject executeBatch(String indexId, String baseUri, HttpServletRequest request) throws Exception {
        log.info(baseUri);

        Map<String, Long> countsByFilename = new HashMap<String, Long>();
        List<JSONObject> refJsons = new ArrayList<JSONObject>();
        extractIndexes(request, countsByFilename, refJsons);

        List<BatchItem> batchesToExecute = new ArrayList<BatchItem>();

        JSONObject json = new JSONObject();

        for (Map.Entry<String, Long> entry : countsByFilename.entrySet()) {
            String filename = entry.getKey();
            Long totalCount = entry.getValue();

            String batchUri = baseUri + "/" + filename;

            JSONObject batchJson = new JSONObject();
            batchJson.put("name", filename);
            batchJson.put("uri", batchUri);
            batchJson.put("numberOfIndexes", totalCount);
            json.append("items", batchJson);

            if (isNewBatch(batchUri)) {
                BatchItem batchItem = new BatchItem(batchUri, indexId, totalCount);
                batchesToExecute.add(batchItem);
                batchesByUri.put(batchUri, batchItem);
            } else {
                batchJson.put("message", "Skipping, this batch is already running");
            }
        }

        LuceneIndexTemplate indexTemplate = getLuceneIndexTemplate(indexId);
        BatchCallback batchCallback = new ReferenceJsonBatchCallback(indexTemplate);
        new Thread(new BatchSplitter(batchCallback, batchesToExecute, refJsons)).start();
        return json;
    }

    private boolean isNewBatch(String batchUri) {
        if (batchesByUri.containsKey(batchUri)) {
            BatchItem batchItem = batchesByUri.get(batchUri);
            if (batchItem.isFinished()) {
                batchesByUri.remove(batchUri);
                return true;
            }
            return false;
        }
        return true;
    }

    private void extractIndexes(HttpServletRequest request, Map<String, Long> fileCounts, List<JSONObject> refJsons) {
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (!isMultipart) {
            log.warning("extractIndexes(): not multipart content");
            return;
        }

        try {
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator itr = upload.getItemIterator(request);
            if (!itr.hasNext()) {
                log.info("extractIndexes(): no files found");
            }

            while (itr.hasNext()) {
                FileItemStream itemStream = itr.next();
                if (!itemStream.isFormField()) {
                    String filename = itemStream.getName();
                    log.info("extractIndexes(): extracting content from:" + filename);

                    long numberOfIndexes = 0;

                    BufferedReader reader = new BufferedReader(new InputStreamReader(itemStream.openStream()));
                    String line = "";
                    while (line != null) {
                        line = reader.readLine();
                        if (line != null) {
                            JSONObject refObj = new JSONObject(line);
                            if (refObj.has("uri")) {
                                refJsons.add(refObj);
                                numberOfIndexes++;
                            }
                        }
                    }
                    fileCounts.put(filename, numberOfIndexes);
                }
            }
        } catch (Exception e) {
            log.warning("extractIndexes(): unable to extract content:" + e);
        }
    }

    private JSONObject getBatchJson(BatchItem batch) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uri", batch.getBatchUri());
        json.put("status", batch.getStatus());
        json.put("message", batch.getMessage());
        json.put("count", batch.getIndexCount());
        json.put("total", batch.getTotalCount());
        return json;
    }
}