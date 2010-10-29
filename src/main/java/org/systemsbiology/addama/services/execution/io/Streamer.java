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
package org.systemsbiology.addama.services.execution.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class Streamer implements Runnable, Closeable {
    private static final Logger log = Logger.getLogger(Streamer.class.getName());

    protected final InputStream inputStream;
    protected final OutputStream outputStream;
    private boolean stop;
    private boolean flushOften;

    public Streamer(InputStream inputStream, OutputStream outputStream) {
        this(inputStream, outputStream, false);
    }

    public Streamer(InputStream inputStream, OutputStream outputStream, boolean flushOften) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.flushOften = flushOften;
    }

    /*
     * Implementation: Runnable
     */

    public void run() {
        try {
            byte[] buffer = new byte[1024];
            while (!stop) {
                int bytesRead = inputStream.read(buffer);
                if (bytesRead == -1) break;

                outputStream.write(buffer, 0, bytesRead);
                if (flushOften) {
                    outputStream.flush();
                }
            }

            inputStream.close();
        } catch (IOException e) {
            log.warning("streamer.run:" + e);
        }

    }

    /*
     * Implementation: Closeable
     */

    public void close() throws IOException {
        this.stop = true;
    }
}
