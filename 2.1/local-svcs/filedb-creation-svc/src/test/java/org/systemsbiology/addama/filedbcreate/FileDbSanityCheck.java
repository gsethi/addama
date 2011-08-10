package org.systemsbiology.addama.filedbcreate;/*
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

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.json.JSONException;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientTemplate;
import org.systemsbiology.addama.commons.httpclient.support.OkJsonResponseCallback;

import java.io.File;

/**
 * @author hrovira
 */
public class FileDbSanityCheck {
    private static final HttpClientTemplate TEMPLATE = new HttpClientTemplate();
    private static final String URL = "http://localhost:8080/filedb-creation-svc/addama/datasources/publictables";

    public static void main(String[] args) throws Exception {
        TEMPLATE.afterPropertiesSet();

        NameValuePair labeledNVP = getTransform("labeledDataMatrix");
        PostMethod labeled = getPost(new File("/local/downloads/patientgenescores.tsv"), labeledNVP);
        System.out.println("labeled=" + TEMPLATE.executeMethod(labeled, new OkJsonResponseCallback()));

        NameValuePair typeMapNVP = getTransform("typeMap");
        PostMethod typeMap = getPost(new File("/local/downloads/track.tsv"), typeMapNVP, getTrackConfig());
        System.out.println("typeMap=" + TEMPLATE.executeMethod(typeMap, new OkJsonResponseCallback()));
    }

    private static PostMethod getPost(File f, NameValuePair... nvps) throws Exception {
        PostMethod post = new PostMethod(URL);
        if (nvps != null) {
            post.setQueryString(nvps);
        }
        post.setRequestEntity(new MultipartRequestEntity(new FilePart[]{new FilePart(f.getName(), f)}, post.getParams()));
        return post;
    }

    private static NameValuePair getTransform(String transform) throws JSONException {
        return new NameValuePair("transform", transform);
    }

    private static NameValuePair getTrackConfig() throws JSONException {
        JSONObject trackConfig = new JSONObject();
        trackConfig.put("sample_id", "text");
        trackConfig.put("chr", "text");
        trackConfig.put("start", "double");
        trackConfig.put("span", "double");
        trackConfig.put("value", "double");
        return new NameValuePair("typeMap", trackConfig.toString());
    }
}
