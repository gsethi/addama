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
package org.systemsbiology.addama.services.execution.jobs;

import java.io.Closeable;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class ExecutionRunnable implements Runnable {
    private static final Logger log = Logger.getLogger(ExecutionRunnable.class.getName());

    private final Process process;
    private final Workspace workspace;
    private final Results results;
    private Closeable[] closeables;

    public ExecutionRunnable(Process p, Workspace w, Results r) {
        this.process = p;
        this.workspace = w;
        this.results = r;
    }

    public void setCloseables(Closeable... cs) {
        this.closeables = cs;
    }

    public void run() {
        try {
            this.workspace.init();
            int result = this.process.waitFor();
            log.info("run: completed with result: " + result);
            this.results.setSuccessful(true);
        } catch (Exception e) {
            this.results.setSuccessful(false);
            this.results.setError(e);
        } finally {
            if (closeables != null) {
                for (Closeable closeable : closeables) {
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        log.warning("endOfProcess:" + e);
                    }
                }
            }
        }
    }
}