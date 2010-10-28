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

import org.systemsbiology.addama.services.execution.util.Mailer;

import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class Executer implements Runnable {
    private static final Logger log = Logger.getLogger(Executer.class.getName());

    private final Process process;
    private final ExecutionCallback executionCallback;
    private final Mailer mailer;

    public Executer(Process process, ExecutionCallback callback, Mailer mailer) {
        this.process = process;
        this.executionCallback = callback;
        this.mailer = mailer;
    }

    public void run() {
        try {
            int result = process.waitFor();
            log.info("run: completed with result: " + result);
            this.executionCallback.onResult(process, result);
            if (this.mailer != null) {
                this.mailer.sendMail();
            }
        } catch (Exception e) {
            this.executionCallback.onError(e);
        }
    }
}
