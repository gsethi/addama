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
package org.systemsbiology.addama.workspaces.fs;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.systemsbiology.addama.commons.httpclient.support.ApiKeyHttpClientTemplate;
import org.systemsbiology.addama.commons.httpclient.support.DirectLinkResponseCallback;
import org.systemsbiology.addama.commons.httpclient.support.GaeHostConfiguration;
import org.systemsbiology.addama.commons.httpclient.support.OkJsonResponseCallback;

import java.io.File;
import java.net.URL;

/**
 * @author hrovira
 */
public class WorkspaceSanityCheck {

    private static final String URI = "/addama/workspaces/edgelimma/hrovira@gmail.com/TestTest";

    private static ApiKeyHttpClientTemplate template;

    public static void main(String[] args) throws Exception {
        GaeHostConfiguration hc = new GaeHostConfiguration();
//        hc.setSecureHostUrl(new URL("https://addama-systemsbiology.appspot.com"));
        hc.afterPropertiesSet();

        HttpClient client = new HttpClient();
        client.setHostConfiguration(hc);

        template = new ApiKeyHttpClientTemplate(client);
//        template.setApikey("7511d0de-5d8e-4050-b6a2-eab7ebe6f33a");
        template.afterPropertiesSet();

        doPost(URI, new File("/local/temp/rest/testakeller.tsv"));
    }

    private static void doPost(String uri, File f) throws Exception {
        String directLink = (String) template.executeMethod(new GetMethod(uri + "/directlink"), new DirectLinkResponseCallback());

        PostMethod post = new PostMethod(directLink);
        post.setRequestEntity(new MultipartRequestEntity(new FilePart[]{new FilePart(f.getName(), f)}, post.getParams()));

        System.out.println("json=" + template.executeMethod(post, new OkJsonResponseCallback()));
    }
}
