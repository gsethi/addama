package org.systemsbiology.google.visualization.datasource;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class TqxParserTest {
    @Test
    public void has_out() {
        TqxParser tqxParser = new TqxParser("version:0.6;reqId:1;sig:5277771;out:json_array;");
        String out = tqxParser.getOut();
        assertEquals("json_array", out);
        assertTrue(StringUtils.equalsIgnoreCase(out, "json_array"));
    }

    @Test
    public void null_tqx() {
        TqxParser tqxParser = new TqxParser(null);
        String out = tqxParser.getOut();
        assertNull(out);
        assertFalse(StringUtils.equalsIgnoreCase(out, "json_array"));
    }

    @Test
    public void empty_tqx() {
        TqxParser tqxParser = new TqxParser("");
        String out = tqxParser.getOut();
        assertNull(out);
        assertFalse(StringUtils.equalsIgnoreCase(out, "json_array"));
    }

    @Test
    public void no_out() {
        TqxParser tqxParser = new TqxParser("version:0.6;reqId:1;sig:5277771;");
        String out = tqxParser.getOut();
        assertNull(out);
        assertFalse(StringUtils.equalsIgnoreCase(out, "json_array"));
    }

    @Test
    public void wrong_out() {
        TqxParser tqxParser = new TqxParser("version:0.6;reqId:1;sig:5277771;out:json;");
        String out = tqxParser.getOut();
        assertNotSame("json_array", out);
        assertFalse(StringUtils.equalsIgnoreCase(out, "json_array"));
    }

    @Test
    public void bad_out() {
        TqxParser tqxParser = new TqxParser("version:0.6;reqId:1;sig:5277771;out;");
        String out = tqxParser.getOut();
        assertNull(out);
        assertFalse(StringUtils.equalsIgnoreCase(out, "json_array"));
    }
}
