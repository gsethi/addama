package org.systemsbiology.addama.coresvcs.gae.asynch;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

/**
 * @author hrovira
 */
public class CaptureOutputResponse extends ServletOutputStream implements HttpServletResponse {
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(10240000);
    private final PrintWriter writer;
    private final HttpServletResponse response;

    private int status = SC_NOT_FOUND;
    private String contentType;

    public CaptureOutputResponse(HttpServletResponse response) {
        this.response = response;
        this.writer = new PrintWriter(this);
    }

    /*
     * Public Methods
     */

    public byte[] getContent() {
        this.writer.flush();
        return this.byteArrayOutputStream.toByteArray();
    }

    /*
     * Overrides ServletResponse
     */
    public ServletOutputStream getOutputStream() throws IOException {
        return this;
    }

    public PrintWriter getWriter() throws IOException {
        return this.writer;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String s) {
        this.contentType = s;
    }

    /*
    * ServletOutputStream
    */

    public void write(int b) throws IOException {
        byteArrayOutputStream.write(b);
    }

    /*
    * Delegate
    */

    public void addCookie(Cookie cookie) {
        response.addCookie(cookie);
    }

    public boolean containsHeader(String s) {
        return response.containsHeader(s);
    }

    public String encodeURL(String s) {
        return response.encodeURL(s);
    }

    public String encodeRedirectURL(String s) {
        return response.encodeRedirectURL(s);
    }

    public String encodeUrl(String s) {
        return response.encodeUrl(s);
    }

    public String encodeRedirectUrl(String s) {
        return response.encodeRedirectUrl(s);
    }

    public void sendError(int i, String s) throws IOException {
        response.sendError(i, s);
    }

    public void sendError(int i) throws IOException {
        response.sendError(i);
    }

    public void sendRedirect(String s) throws IOException {
        response.sendRedirect(s);
    }

    public void setDateHeader(String s, long l) {
        response.setDateHeader(s, l);
    }

    public void addDateHeader(String s, long l) {
        response.addDateHeader(s, l);
    }

    public void setHeader(String s, String s1) {
        response.setHeader(s, s1);
    }

    public void addHeader(String s, String s1) {
        response.addHeader(s, s1);
    }

    public void setIntHeader(String s, int i) {
        response.setIntHeader(s, i);
    }

    public void addIntHeader(String s, int i) {
        response.addIntHeader(s, i);
    }

    public void setStatus(int i) {
        this.status = i;
    }

    public void setStatus(int i, String s) {
        this.status = i;
    }

    public String getCharacterEncoding() {
        return response.getCharacterEncoding();
    }

    public void setCharacterEncoding(String s) {
        response.setCharacterEncoding(s);
    }

    public void setContentLength(int i) {
        response.setContentLength(i);
    }

    public void setBufferSize(int i) {
        response.setBufferSize(i);
    }

    public int getBufferSize() {
        return response.getBufferSize();
    }

    public void flushBuffer() throws IOException {
        response.flushBuffer();
    }

    public void resetBuffer() {
        response.resetBuffer();
    }

    public boolean isCommitted() {
        return response.isCommitted();
    }

    public void reset() {
        response.reset();
    }

    public void setLocale(Locale locale) {
        response.setLocale(locale);
    }

    public Locale getLocale() {
        return response.getLocale();
    }
    /*
    * Servlet API 3.0
    */

    public int getStatus() {
        return status;
    }

    public String getHeader(String s) {
        return null;
    }

    public Collection<String> getHeaders(String s) {
        return new ArrayList<String>();
    }

    public Collection<String> getHeaderNames() {
        return new ArrayList<String>();
    }
}
