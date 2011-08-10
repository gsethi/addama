package org.systemsbiology.addama.services.proxy.transforms;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author hrovira
 */
public interface ResponseTransform {
    void handle(InputStream inputStream, HttpServletResponse response) throws Exception;
}
