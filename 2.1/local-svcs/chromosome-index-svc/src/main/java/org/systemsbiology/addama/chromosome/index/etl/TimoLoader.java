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
package org.systemsbiology.addama.chromosome.index.etl;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.systemsbiology.addama.commons.httpclient.support.ApiKeyHttpClientTemplate;
import org.systemsbiology.addama.commons.httpclient.support.DirectLinkResponseCallback;
import org.systemsbiology.addama.commons.httpclient.support.GaeHostConfiguration;
import org.systemsbiology.addama.commons.httpclient.support.OkJsonResponseCallback;

import java.io.*;
import java.net.URL;

/**
 * @author hrovira
 */
public class TimoLoader {
    private static final String SEPARATOR = "\t";
    private static ApiKeyHttpClientTemplate template;

    public static void main(String[] args) throws Exception {
        File inputHg18 = new File("/local/refgenomes/hg18_coord_vs_cytobandID.tsv");
        File outputHg18 = new File("/local/refgenomes/locus_hg18.tsv");

        File inputHg19 = new File("/local/refgenomes/hg19_coord_vs_cytobandID.tsv");
        File outputHg19 = new File("/local/refgenomes/locus_hg19.tsv");

        doTransform(inputHg18, outputHg18);
        doTransform(inputHg19, outputHg19);

        initTemplate();

        File schemaHg18 = new File("/local/refgenomes/locus_schema_hg18.json");
        doSubmit("/addama/repositories/workspaces/hrovira@systemsbiology.org/refgenome/locus/hg18", outputHg18);
        doSubmit("/addama/repositories/workspaces/hrovira@systemsbiology.org/refgenome/locus/hg18/datasources", schemaHg18);

        File schemaHg19 = new File("/local/refgenomes/locus_schema_hg19.json");
        doSubmit("/addama/repositories/workspaces/hrovira@systemsbiology.org/refgenome/locus/hg19", outputHg19);
        doSubmit("/addama/repositories/workspaces/hrovira@systemsbiology.org/refgenome/locus/hg19/datasources", schemaHg19);
    }

    private static void doTransform(File input, File output) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(input)));

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output)));
        writer.write(getOutputHeader());
        writer.newLine();

        String line = reader.readLine();
        while (line != null) {
            line = reader.readLine();
            if (line != null) {
                String[] splits = line.split(SEPARATOR);
                String chromosome = splits[0];
                Integer start = Integer.parseInt(splits[1]);
                Integer end = Integer.parseInt(splits[2]);
                String locus = StringUtils.substringAfterLast(chromosome, "chr") + splits[3];
                String uri = "/addama/refgenome/hg18/locus/" + locus;


                writer.write(chromosome + SEPARATOR);
                writer.write(start + SEPARATOR);
                writer.write(end + SEPARATOR);
                writer.write(locus + SEPARATOR);
                writer.write(uri);
                writer.newLine();

//                locusSchema: {
//                                table: "LOCUS_URI_CHROM_INDEX",
//                                chromosome: "chrom",
//                                start: "start",
//                                end: "end",
//                                locus: "locus",
//                                uri: "uri"
//                            }
            }
        }
    }

    private static void initTemplate() throws Exception {
        GaeHostConfiguration hc = new GaeHostConfiguration();
        hc.setSecureHostUrl(new URL("https://addama-systemsbiology-public.appspot.com"));
        hc.afterPropertiesSet();

        HttpClient client = new HttpClient();
        client.setHostConfiguration(hc);

        template = new ApiKeyHttpClientTemplate(client);
        template.setApikey("60667408-363a-45a5-b771-42a8e4ecc0a7");
        template.afterPropertiesSet();

    }

    private static void doSubmit(String uri, File f) throws Exception {
        String directLink = (String) template.executeMethod(new GetMethod(uri + "/directlink"), new DirectLinkResponseCallback());

        PostMethod post = new PostMethod(directLink);
        post.setRequestEntity(new MultipartRequestEntity(new FilePart[]{new FilePart(f.getName(), f)}, post.getParams()));

        System.out.println("json=" + template.executeMethod(post, new OkJsonResponseCallback()));
    }


    private static String getOutputHeader() {
        StringBuilder builder = new StringBuilder();
        builder.append("chrom").append(SEPARATOR);
        builder.append("start").append(SEPARATOR);
        builder.append("end").append(SEPARATOR);
        builder.append("locus").append(SEPARATOR);
        builder.append("uri");
        return builder.toString();
    }
}
