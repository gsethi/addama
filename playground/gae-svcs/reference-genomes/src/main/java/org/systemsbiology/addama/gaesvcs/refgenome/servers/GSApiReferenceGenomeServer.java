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
package org.systemsbiology.addama.gaesvcs.refgenome.servers;

import com.google.appengine.api.urlfetch.*;
import org.apache.commons.lang.StringUtils;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.gaesvcs.refgenome.ReferenceGenomeServer;

import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * @author hrovira
 */
public class GSApiReferenceGenomeServer implements ReferenceGenomeServer {
    private final URLFetchService fetchService = URLFetchServiceFactory.getURLFetchService();

    private Map<String, String> bucketObjectUriByChromosome;
    private URL bucketUrl;

    /*
     * Dependency Injection
     */

    public void setBucketObjectUriByChromosome(Map<String, String> map) {
        this.bucketObjectUriByChromosome = map;
    }

    public void setBucketUrl(URL bucketUrl) {
        this.bucketUrl = bucketUrl;
    }

    /*
     * IReferenceGenomeServer
     */

    public String[] getChromosomes() {
        if (bucketObjectUriByChromosome != null && !bucketObjectUriByChromosome.isEmpty()) {
            Set<String> chromosomes = bucketObjectUriByChromosome.keySet();
            return chromosomes.toArray(new String[chromosomes.size()]);
        }
        return new String[0];
    }

    public Long getChromosomeLength(String chromosome) throws Exception {
        String bucketObjectUri = bucketObjectUriByChromosome.get(chromosome);
        if (StringUtils.isEmpty(bucketObjectUri)) {
            throw new ResourceNotFoundException(bucketUrl + bucketObjectUri);
        }

        URL requestUrl = new URL(bucketUrl.toString() + bucketObjectUri);
        HTTPRequest request = new HTTPRequest(requestUrl, HTTPMethod.HEAD);
        request.addHeader(new HTTPHeader("Host", bucketUrl.getHost()));
        request.addHeader(new HTTPHeader("Date", getCurrentDateGMT()));

        HTTPResponse response = fetchService.fetch(request);
        for (HTTPHeader header : response.getHeaders()) {
            if (StringUtils.equalsIgnoreCase(header.getName(), "Content-Length")) {
                return Long.parseLong(header.getValue());
            }
        }
        return (long) response.getContent().length;
    }

    public void loadSequence(OutputStream outputStream, String chromosome, long start, long end) throws Exception {
        String bucketObjectUri = bucketObjectUriByChromosome.get(chromosome);
        if (StringUtils.isEmpty(bucketObjectUri)) {
            throw new ResourceNotFoundException(bucketUrl + bucketObjectUri);
        }

        URL requestUrl = new URL(bucketUrl.toString() + bucketObjectUri);
        HTTPRequest request = new HTTPRequest(requestUrl, HTTPMethod.GET);
        request.addHeader(new HTTPHeader("Range", "bytes=" + start + "-" + end));
        request.addHeader(new HTTPHeader("Host", bucketUrl.getHost()));
        request.addHeader(new HTTPHeader("Date", getCurrentDateGMT()));

        HTTPResponse response = fetchService.fetch(request);
        if (response != null) {
            outputStream.write(response.getContent());
            outputStream.flush();
        }
    }

    /* 
     * Private Methods
     */

    private String getCurrentDateGMT() {
        Calendar gmtcal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, gmtcal.get(Calendar.HOUR_OF_DAY));

        SimpleDateFormat sdf = new SimpleDateFormat("E',' dd MMM yyyy HH:mm:ss 'GMT'");
        return sdf.format(cal.getTime());
    }
}
