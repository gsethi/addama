package org.systemsbiology.addama.experimental.asynch;

/**
 * @author hrovira
 */
public class AsynchRequestException extends RuntimeException {
    public AsynchRequestException(String uri) {
        super("asynch request for:" + uri);
    }
}
