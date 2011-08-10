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

    static {
        try {
            requestUriMethod = HttpServletRequest.class.getMethod("getRequestURI", null);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    public static HttpServletRequest instrumentRequest(HttpServletRequest request, String originalRequestUri) {
        Class[] proxyInterfaces = new Class[]{HttpServletRequest.class};
        ClassLoader cl = HttpServletRequest.class.getClassLoader();
        InvocationHandler invocationHandler = new HttpRequestUriInvocationHandler(request, originalRequestUri);
        return (HttpServletRequest) newProxyInstance(cl, proxyInterfaces, invocationHandler);
    }

    private final HttpServletRequest request;
    private final String originalRequestUri;

    public HttpRequestUriInvocationHandler(HttpServletRequest request, String originalRequestUri) {
        this.request = request;
        this.originalRequestUri = originalRequestUri;
    }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        if (m.getDeclaringClass() == HttpServletRequest.class) {
            if (m.equals(requestUriMethod)) {
                return originalRequestUri;
            }
        }

        try {
            return m.invoke(request, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
