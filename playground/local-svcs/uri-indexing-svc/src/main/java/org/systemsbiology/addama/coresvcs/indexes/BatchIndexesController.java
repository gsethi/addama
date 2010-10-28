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
package org.systemsbiology.addama.coresvcs.indexes;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.lucene.index.core.LuceneIndexTemplate;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.ResourceNotFoundView;
import org.systemsbiology.addama.coresvcs.indexes.batches.BatchCallback;
import org.systemsbiology.addama.coresvcs.indexes.batches.BatchItem;
import org.systemsbiology.addama.coresvcs.indexes.batches.BatchSplitter;
import org.systemsbiology.addama.coresvcs.indexes.batches.ReferenceJsonBatchCallback;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class BatchIndexesController extends BaseIndexingController {
    private static final Logger log = Logger.getLogger(BatchIndexesController.class.getName());

    // TODO : Persist batches
    private static final Map<String, BatchItem> batchesByUri = new HashMap<String, BatchItem>();

    @RequestMapping(value = "**/batches", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView batches(HttpServletRequest request) throws Exception {
        log.info("batches(" + request.getRequestURI() + ")");

        String baseUri = StringUtils.substringAfterLast(request.getRequestURI(), request.getContextPath());
        if (baseUri.endsWith("/")) {
            baseUri = StringUtils.substringBeforeLast(baseUri, "/");
        }

        ModelAndView mav = new ModelAndView(new JsonItemsView());
        mav.addObject("json", executeBatch(baseUri, request));
        return mav;
    }

    @RequestMapping(value = "**/batches", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getBatches(HttpServletRequest request) throws Exception {
        log.info("getBatches(" + request.getRequestURI() + ")");

        JSONObject json = new JSONObject();
        for (BatchItem batchItem : batchesByUri.values()) {
            json.append("items", getBatchJson(batchItem));
        }

        ModelAndView mav = new ModelAndView(new JsonItemsView());
        mav.addObject("json", json);
        return mav;
    }

    @RequestMapping(value = "**/batches/*", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getBatch(HttpServletRequest request) throws Exception {
        String batchUri = StringUtils.substringAfter(request.getRequestURI(), request.getContextPath());

        BatchItem batch = batchesByUri.get(batchUri);
        if (batch == null) {
            return new ModelAndView(new ResourceNotFoundView());
        }

        JSONObject json = getBatchJson(batch);

        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", json);
        return mav;
    }

    /*
     * Private Methods
     */

    private JSONObject executeBatch(String baseUri, HttpServletRequest request) throws Exception {
        log.info("executeBatch(" + baseUri + ")");

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
                String rootUri = StringUtils.substringBetween(batchUri, "/addama/indexes", "/batches");
                BatchItem batchItem = new BatchItem(batchUri, rootUri, totalCount);
                batchesToExecute.add(batchItem);
                batchesByUri.put(batchUri, batchItem);
            } else {
                batchJson.put("message", "Skipping, this batch is already running");
            }
        }

        LuceneIndexTemplate indexTemplate = getLuceneIndexTemplate(request);
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