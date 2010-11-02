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

import org.systemsbiology.addama.services.execution.dao.JobUpdater;
import org.systemsbiology.addama.services.execution.util.Mailer;

import java.io.Closeable;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class Executer implements Runnable {
    private static final Logger log = Logger.getLogger(Executer.class.getName());

    private final Process process;
    private final JobUpdater jobUpdater;
    private final Mailer mailer;
    private final Closeable[] closeables;

    public Executer(Process process, JobUpdater jobUpdater, Mailer mailer, Closeable... closeables) {
        this.process = process;
        this.jobUpdater = jobUpdater;
        this.mailer = mailer;
        this.closeables = closeables;
    }

    public void run() {
        try {
            this.jobUpdater.running();

            int result = process.waitFor();
            log.info("run: completed with result: " + result);

            this.jobUpdater.completed();

            if (this.mailer != null) {
                this.mailer.sendMail();
            }
        } catch (Exception e) {
            this.jobUpdater.onError(e);

        } finally {
            closeResources();
        }
    }

    /*
     * Private Methods
     */

    private void closeResources() {
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    log.warning(e.getMessage());
                }
            }
        }
    }
}
