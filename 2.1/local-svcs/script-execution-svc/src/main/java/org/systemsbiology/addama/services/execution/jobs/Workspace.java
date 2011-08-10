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
package org.systemsbiology.addama.services.execution.jobs;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.httpclient.support.*;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class Workspace implements Closeable {
    private static final Logger log = Logger.getLogger(Workspace.class.getName());

    private final HttpClientTemplate httpClientTemplate;
    private final String jobUri;
    private final String workspaceUri;
    private final File workspaceDir;
    private final File uploadsDir;

    public Workspace(HttpClientTemplate httpClientTemplate, String jobUri, String workspaceUri, File workspaceDir, File uploadsDir) throws Exception {
        this.httpClientTemplate = httpClientTemplate;
        this.jobUri = jobUri;
        this.workspaceUri = workspaceUri;
        this.workspaceDir = workspaceDir;
        this.uploadsDir = uploadsDir;
    }

    /*
     * Public Methods
     */

    public void init() throws Exception {
        try {
            JSONObject json = new JSONObject().put("uri", jobUri);

            PostMethod post = new PostMethod(workspaceUri + "/jobs/" + StringUtils.substringAfterLast(jobUri, "/"));
            post.setQueryString(new NameValuePair[]{new NameValuePair("annotations", json.toString())});
            int statusCode = (Integer) httpClientTemplate.executeMethod(post, new StatusCodeCaptureResponseCallback());
            log.info("registerJob(" + workspaceUri + "," + jobUri + "):" + statusCode);
        } catch (Exception e) {
            log.warning("unable to register job " + jobUri + " in workspace " + workspaceUri);
        }

        GetMethod get = new GetMethod(workspaceUri);
        JSONObject json = (JSONObject) httpClientTemplate.executeMethod(get, new OkJsonResponseCallback());
        if (json != null && json.has("items")) {
            JSONArray files = json.getJSONArray("items");
            if (files.length() > 0) {
                for (int i = 0; i < files.length(); i++) {
                    JSONObject jsonFile = files.getJSONObject(i);
                    if (jsonFile.optBoolean("isFile", false)) {
                        File file = new File(workspaceDir, jsonFile.getString("name"));
                        GetMethod getFile = new GetMethod(jsonFile.getString("uri"));
                        getFile.setFollowRedirects(true);
                        httpClientTemplate.executeMethod(getFile, new PipeInputStreamContentResponseCallback(file));
                    }
                }
            }
        }
    }

    public void close() throws IOException {
        Map<String, List<File>> filesByUri = new HashMap<String, List<File>>();
        addFiles(uploadsDir, filesByUri, workspaceUri);

        List<String> filePaths = new ArrayList<String>(filesByUri.keySet());
        Collections.sort(filePaths);
        for (String filePath : filePaths) {
            try {
                String uri = filePath + "/directlink";
                log.info("uri=" + uri);

                List<File> files = filesByUri.get(filePath);
                for (File f : files) {
                    String link = (String) httpClientTemplate.executeMethod(new GetMethod(uri), new DirectLinkResponseCallback());
                    PostMethod post = new PostMethod(link);
                    post.setRequestEntity(new MultipartRequestEntity(new Part[]{new FilePart(f.getName(), f)}, post.getParams()));
                    httpClientTemplate.executeMethod(post, new StatusCodeCaptureResponseCallback());
                }
            } catch (Exception e) {
                log.warning("close:" + e);
            }
        }
    }

    /*
    * Private Methods
    */

    private void addFiles(File dir, Map<String, List<File>> filesByUri, String referencePath) {
        if (dir != null && dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                String newrefpath = referencePath + "/" + f.getName();
                if (f.isFile()) {
                    List<File> files = filesByUri.get(referencePath);
                    if (files == null) {
                        files = new ArrayList<File>();
                        filesByUri.put(referencePath, files);
                    }
                    files.add(f);
                }
                addFiles(f, filesByUri, newrefpath);
            }
        }
    }

}
