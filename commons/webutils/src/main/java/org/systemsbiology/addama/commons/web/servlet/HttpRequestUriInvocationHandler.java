package org.systemsbiology.addama.commons.web.servlet;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.reflect.Proxy.newProxyInstance;

/**
 * @author hrovira
 */
public class HttpRequestUriInvocationHandler implements InvocationHandler {
    private static Method requestUriMethod;
    private static Method requestHeaderMethod;

    static {
        try {
            requestUriMethod = HttpServletRequest.class.getMethod("getRequestURI", null);
            requestHeaderMethod = HttpServletRequest.class.getMethod("getHeader", String.class);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    public static HttpServletRequest instrumentRequest(HttpServletRequest request, String originalRequestUri, String originalRequester) {
        Class[] proxyInterfaces = new Class[]{HttpServletRequest.class};
        ClassLoader cl = HttpServletRequest.class.getClassLoader();
        InvocationHandler invocationHandler = new HttpRequestUriInvocationHandler(request, originalRequestUri, originalRequester);
        return (HttpServletRequest) newProxyInstance(cl, proxyInterfaces, invocationHandler);
    }

    private final HttpServletRequest request;
    private final String originalRequestUri;
    private final String originalRequester;

    public HttpRequestUriInvocationHandler(HttpServletRequest request, String originalRequestUri, String originalRequester) {
        this.request = request;
        this.originalRequestUri = originalRequestUri;
        this.originalRequester = originalRequester;
    }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        if (m.getDeclaringClass() == HttpServletRequest.class) {
            if (m.equals(requestUriMethod)) {
                return originalRequestUri;
            }
            if (m.equals(requestHeaderMethod)) {
                if ("x-addama-registry-user".equalsIgnoreCase((String) args[0])) {
                    return originalRequester;
                }
            }
        }

        try {
            return m.invoke(request, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
