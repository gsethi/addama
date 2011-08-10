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
package org.systemsbiology.addama.commons.httpclient.support;

import org.apache.commons.httpclient.HttpMethod;

import java.io.*;

/**
 * @author hrovira
 */
public final class PipeInputStreamContentResponseCallback extends InputStreamResponseCallback {
    private final OutputStream outputStream;
    private int bufferSize = 1024;

    public PipeInputStreamContentResponseCallback(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public PipeInputStreamContentResponseCallback(OutputStream outputStream, int bufferSize) {
        this.outputStream = outputStream;
        this.bufferSize = bufferSize;
    }

    public PipeInputStreamContentResponseCallback(File file) throws FileNotFoundException {
        this.outputStream = new FileOutputStream(file, false);
    }

    public PipeInputStreamContentResponseCallback(File file, int bufferSize) throws FileNotFoundException {
        this.outputStream = new FileOutputStream(file, false);
        this.bufferSize = bufferSize;
    }

    public Object onResponse(int statusCode, HttpMethod method, InputStream inputStream) throws Exception {
        byte[] buffer = new byte[this.bufferSize];
        while (true) {
            int bytesRead = inputStream.read(buffer);
            if (bytesRead == -1) break;
            outputStream.write(buffer, 0, bytesRead);
        }
        return null;
    }
}