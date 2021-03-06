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
package org.systemsbiology.addama.services.execution.util;

import org.json.JSONException;
import org.systemsbiology.addama.services.execution.jobs.Job;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.*;

/**
 * @author hrovira
 */
public class HttpJob {
    private static final Logger log = Logger.getLogger(HttpJob.class.getName());

    public static File[] getOutputFiles(Job job) throws JSONException {
        List<File> outputs = new ArrayList<File>();
        scanOutputs(outputs, new File(job.getOutputDirectoryPath()));
        return outputs.toArray(new File[outputs.size()]);
    }

    public static void scanOutputs(List<File> outputs, File outputDir) {
        if (outputDir.isFile()) {
            outputs.add(outputDir);
        }
        if (outputDir.isDirectory()) {
            for (File f : outputDir.listFiles()) {
                scanOutputs(outputs, f);
            }
        }
    }

    public static String getUserUri(HttpServletRequest request) {
        String userUri = request.getHeader("x-addama-registry-user");
        if (!isEmpty(userUri)) {
            return userUri;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (equalsIgnoreCase("x-addama-registry-user", cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    public static String getUser(HttpServletRequest request) {
        String userUri = request.getHeader("x-addama-registry-user");
        if (!isEmpty(userUri)) {
            return substringAfterLast(userUri, "/addama/users/");
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (equalsIgnoreCase("x-addama-registry-user", cookie.getName())) {
                    return substringAfterLast(cookie.getValue(), "/addama/users/");
                }
            }
        }

        return null;
    }

    public static String[] getScriptExecution(Job job) {
        ArrayList<String> exec = new ArrayList<String>();
        String scriptPath = job.getScriptPath();
        if (!isEmpty(scriptPath)) {
            exec.addAll(asList(job.getScriptPath().split(" ")));
        }
        if (!isEmpty(job.getScriptArgs())) {
            exec.add(job.getScriptArgs());
        }
        log.fine("args=" + exec);
        return exec.toArray(new String[exec.size()]);
    }

}