package org.apache.jackrabbit.extractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * @author hrovira
 */
public class PlainTextExtractor2  extends AbstractTextExtractor {
    private static final Logger logger = LoggerFactory.getLogger(PlainTextExtractor2.class);

    /**
     * Creates a new <code>PlainTextExtractor</code> instance.
     */
    public PlainTextExtractor2() {
        super(new String[]{"text/plain"});
    }

    //-------------------------------------------------------< TextExtractor >

    /**
     * Wraps the given input stream to an {@link java.io.InputStreamReader} using
     * the given encoding, or the platform default encoding if the encoding
     * is not given. Closes the stream and returns an empty reader if the
     * given encoding is not supported.
     *
     * @param stream binary stream
     * @param type ignored
     * @param encoding character encoding, optional
     * @return reader for the plain text content
     * @throws java.io.IOException if the binary stream can not be closed in case
     *                     of an encoding issue
     */
    public Reader extractText(InputStream stream, String type, String encoding)
            throws IOException {
        try {
            if (encoding != null && !encoding.equals("")) {
                return new InputStreamReader(stream, encoding);
            } else {
                return new InputStreamReader(stream);
            }
        } catch (UnsupportedEncodingException e) {
            logger.warn("Failed to extract plain text content", e);
            stream.close();
            return new StringReader("");
        }
    }

}
