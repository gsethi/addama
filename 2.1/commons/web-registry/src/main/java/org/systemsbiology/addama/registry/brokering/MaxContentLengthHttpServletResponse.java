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
package org.systemsbiology.addama.registry.brokering;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

/**
 * @author hrovira
 */
public class MaxContentLengthHttpServletResponse extends ServletOutputStream implements HttpServletResponse {
    private final PrintWriter writer;
    private final HttpServletResponse response;
    private final Integer maxContentLength;
    private int numberOfBytes;
    private boolean limitExceeded;

    public MaxContentLengthHttpServletResponse(HttpServletResponse response, Integer maxContentLength) {
        this.response = response;
        this.writer = new PrintWriter(this);
        this.maxContentLength = maxContentLength;
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

    public boolean isLimitExceeded() {
        return limitExceeded;
    }

    /*
    * ServletOutputStream
    */

    public void write(int b) throws IOException {
        if (this.limitExceeded) {
            return;
        }

        if (this.numberOfBytes++ >= this.maxContentLength) {
            this.limitExceeded = true;
        }
    }

    /*
    * Wrapper Methods
    */

    public void addCookie(Cookie cookie) {
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
    }

    public void sendError(int i) throws IOException {
    }

    public void sendRedirect(String s) throws IOException {
    }

    public void setDateHeader(String s, long l) {
    }

    public void addDateHeader(String s, long l) {
    }

    public void setHeader(String s, String s1) {
    }

    public void addHeader(String s, String s1) {
    }

    public void setIntHeader(String s, int i) {
    }

    public void addIntHeader(String s, int i) {
    }

    public void setStatus(int i) {
    }

    public void setStatus(int i, String s) {
    }

    public String getCharacterEncoding() {
        return response.getCharacterEncoding();
    }

    public String getContentType() {
        return response.getContentType();
    }

    public void setContentType(String contentType) {
    }

    public void setCharacterEncoding(String s) {
    }

    public void setContentLength(int i) {
    }

    public void setBufferSize(int i) {
    }

    public int getBufferSize() {
        return response.getBufferSize();
    }

    public void flushBuffer() throws IOException {
    }

    public void resetBuffer() {
    }

    public boolean isCommitted() {
        return response.isCommitted();
    }

    public void reset() {
    }

    public void setLocale(Locale locale) {
    }

    public Locale getLocale() {
        return response.getLocale();
    }

    /*
    * Servlet API 3.0
    */

    public int getStatus() {
        return 200;
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
