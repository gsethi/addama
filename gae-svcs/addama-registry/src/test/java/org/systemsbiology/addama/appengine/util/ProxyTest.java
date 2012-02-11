package org.systemsbiology.addama.appengine.util;

import com.google.appengine.api.urlfetch.HTTPRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;
import static org.apache.commons.lang.StringUtils.replace;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.systemsbiology.addama.appengine.util.Proxy.collectRequestParameters;
import static org.systemsbiology.addama.appengine.util.Proxy.getWithParams;

/**
 * @author hrovira
 */
public class ProxyTest {
    private MockHttpServletRequest GET;

    @Before
    public void setup() {
        GET = new MockHttpServletRequest();
//        https://cancerregulome-tcga-gdac.appspot.com/addama/datasources/internal_tcga/v_gbm_06feb_neura_pw_patient_values/query?tq=select%20f1alias,%20f1values%2C%20f2alias%2C%20f2values%20where%20f1alias%20%20%3D%20'N%3AGEXP%3AHEPH%3AchrX%3A65325101%3A65403239%3A%2B%3A'%20and%20f2alias%20%3D%20'N%3AMIRN%3Ahsa-miR-130b%3Achr22%3A20337593%3A20337674%3A%2B%3A'%20limit%201&tqx=out%3Ajson_array&_dc=1328720355498
    }

    @Test
    public void encoded_collectRequestParameters() throws UnsupportedEncodingException {
        Map<String, String> expected = new HashMap<String, String>();
        expected.put("tq", "select%20f1alias%2C%20f1values%2C%20f2alias%2C%20f2values%20where%20f1alias%20%20%3D%20'N%3AGEXP%3AHEPH%3AchrX%3A65325101%3A65403239%3A%2B%3A'%20and%20f2alias%20%3D%20'N%3AMIRN%3Ahsa-miR-130b%3Achr22%3A20337593%3A20337674%3A%2B%3A'%20limit%201");
        expected.put("tqx", "out%3Ajson_array");

        // simulate appengine decoding parameters on request
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            GET.addParameter(entry.getKey(), decode(entry.getValue(), "UTF-8"));
        }

        Iterable<String> pairs = collectRequestParameters(GET);
        assertTrue(pairs.iterator().hasNext());

        int numberOfPairs = 0;
        for (String pair : pairs) {
            String key = substringBefore(pair, "=");
            String value = substringAfter(pair, key + "=");
            assertTrue(expected.containsKey(key));

            String expectedValue = expected.get(key);
            // Expected encodings
            expectedValue = replace(expectedValue, "%20", "+");
            expectedValue = replace(expectedValue, "'", "%27");

            assertEquals(key, expectedValue, value);
            numberOfPairs++;
        }
        assertEquals(expected.size(), numberOfPairs);
    }

    @Test
    public void unencoded_collectRequestParameters() throws UnsupportedEncodingException {
        Map<String, String> expected = new HashMap<String, String>();
        expected.put("tq", "select f1alias, f1values, f2alias, f2values where f1alias  = 'N:GEXP:HEPH:chrX:65325101:65403239:%2B:' and f2alias = 'N:MIRN:hsa-miR-130b:chr22:20337593:20337674:%2B:' limit 1");
        expected.put("tqx", "out:json_array");

        // simulate appengine decoding parameters on request
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            GET.addParameter(entry.getKey(), decode(entry.getValue(), "UTF-8"));
        }

        Iterable<String> pairs = collectRequestParameters(GET);
        assertTrue(pairs.iterator().hasNext());

        int numberOfPairs = 0;
        for (String pair : pairs) {
            String key = substringBefore(pair, "=");
            String value = substringAfter(pair, key + "=");
            assertTrue(expected.containsKey(key));

            String expectedValue = expected.get(key);
            // Expected encodings
            expectedValue = replace(expectedValue, " ", "+");
            expectedValue = replace(expectedValue, ",", "%2C");
            expectedValue = replace(expectedValue, ":", "%3A");
            expectedValue = replace(expectedValue, "=", "%3D");
            expectedValue = replace(expectedValue, "'", "%27");

            assertEquals(key, expectedValue, value);
            numberOfPairs++;
        }
        assertEquals(expected.size(), numberOfPairs);
    }

    @Test
    public void good_getWithParams() throws MalformedURLException {
        HTTPRequest request = getWithParams(GET, new URL("https://cancerregulome-tcga-gdac.systemsbiology.net/addama/datasources/internal_tcga/v_gbm_06feb_neura_pw_patient_values/query"));
        assertNotNull(request);

        URL actualUrl = request.getURL();
        assertNotNull(actualUrl);
    }
}
