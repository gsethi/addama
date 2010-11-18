package org.apache.jackrabbit.extractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

/**
 * @author hrovira
 */
public class XMLTextExtractor2  extends AbstractTextExtractor {
    private static final Logger logger = LoggerFactory.getLogger(XMLTextExtractor2.class);

    public XMLTextExtractor2() {
        super(new String[]{"text/xml", "application/xml"});
    }

    //-------------------------------------------------------< TextExtractor >

    /**
     * Returns a reader for the text content of the given XML document.
     * Returns an empty reader if the given encoding is not supported or
     * if the XML document could not be parsed.
     *
     * @param stream XML document
     * @param type XML content type
     * @param encoding character encoding, or <code>null</code>
     * @return reader for the text content of the given XML document,
     *         or an empty reader if the document could not be parsed
     * @throws java.io.IOException if the XML document stream can not be closed
     */
    public Reader extractText(InputStream stream, String type, String encoding)
            throws IOException {
        try {
            CharArrayWriter writer = new CharArrayWriter();
            ExtractorHandler handler = new ExtractorHandler(writer);

            // TODO: Use a pull parser to avoid the memory overhead
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);

            // It is unspecified whether the XML parser closes the stream when
            // done parsing. To ensure that the stream gets closed just once,
            // we prevent the parser from closing it by catching the close()
            // call and explicitly close the stream in a finally block.
            InputSource source = new InputSource(new FilterInputStream(stream) {
                public void close() {
                }
            });
            if (encoding != null && !encoding.equals("")) {
                source.setEncoding(encoding);
            }
            reader.parse(source);

            return new CharArrayReader(writer.toCharArray());
        } catch (ParserConfigurationException e) {
            logger.warn("Failed to extract XML text content", e);
            return new StringReader("");
        } catch (SAXException e) {
            logger.warn("Failed to extract XML text content", e);
            return new StringReader("");
        } finally {
            stream.close();
        }
    }

}
