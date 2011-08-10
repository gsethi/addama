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
import org.apache.commons.lang.math.IntRange;
import org.apache.commons.lang.math.Range;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.gaesvcs.refgenome.ReferenceGenomeServer;

import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static com.google.appengine.api.urlfetch.HTTPMethod.GET;
import static com.google.appengine.api.urlfetch.HTTPMethod.HEAD;
import static java.lang.Long.parseLong;
import static java.util.Calendar.getInstance;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isEmpty;

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
        HTTPRequest request = createHTTPrequest(chromosome, null);
        HTTPResponse response = fetchService.fetch(request);
        for (HTTPHeader header : response.getHeaders()) {
            if (equalsIgnoreCase(header.getName(), "Content-Length")) {
                return parseLong(header.getValue());
            }
        }
        return (long) response.getContent().length;
    }

    public void loadSequence(OutputStream outputStream, String chromosome, long start, long end) throws Exception {
        HTTPRequest request = createHTTPrequest(chromosome, new IntRange(start, end));
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
        Calendar gmtcal = getInstance(TimeZone.getTimeZone("GMT"));

        Calendar cal = getInstance();
        cal.set(Calendar.HOUR_OF_DAY, gmtcal.get(Calendar.HOUR_OF_DAY));

        SimpleDateFormat sdf = new SimpleDateFormat("E',' dd MMM yyyy HH:mm:ss 'GMT'");
        return sdf.format(cal.getTime());
    }

    private HTTPRequest createHTTPrequest(String chromosome, Range r) throws Exception {
        String bucketObjectUri = bucketObjectUriByChromosome.get(chromosome);
        if (isEmpty(bucketObjectUri)) {
            throw new ResourceNotFoundException(bucketUrl + "/" + bucketObjectUri);
        }

        URL requestUrl = new URL(bucketUrl.toString() + "/" + bucketObjectUri);
        HTTPRequest request = new HTTPRequest(requestUrl, HEAD);
        if (r != null) {
            request = new HTTPRequest(requestUrl, GET);
            request.addHeader(new HTTPHeader("Range", "bytes=" + r.getMinimumInteger() + "-" + r.getMaximumInteger()));
        }

        request.addHeader(new HTTPHeader("Host", bucketUrl.getHost()));
        request.addHeader(new HTTPHeader("Date", getCurrentDateGMT()));
        return request;
    }
}
