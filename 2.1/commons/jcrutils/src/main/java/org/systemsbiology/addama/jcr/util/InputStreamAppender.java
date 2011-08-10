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
package org.systemsbiology.addama.jcr.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class InputStreamAppender implements Runnable {
    private static final Logger log = Logger.getLogger(InputStreamAppender.class.getName());

    private ArrayList<InputStream> inputStreams = new ArrayList<InputStream>();
    private boolean finished = false;

    private final OutputStream outputStream;

    public InputStreamAppender(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void run() {
        try {
            for (InputStream inputStream : this.inputStreams) {
                log.info("logging input stream");

                byte[] buffer = new byte[1024];
                while (true) {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead == -1) break;
                    this.outputStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            finishStreaming();
        }
    }

    public void append(InputStream... streams) {
        this.inputStreams.addAll(Arrays.asList(streams));
    }

    public boolean isFinishedStreaming() {
        return this.finished;
    }

    public void waitForStreamToFinish() {
        while (!isFinishedStreaming()) {
            try {
                log.info("stream appender not finished");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void finishStreaming() {
        try {
            this.outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            this.finished = true;
            log.info("finishStreaming: " + this.finished);
        }
    }
}
