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
package org.systemsbiology.addama.services.jobsdao.util;

import static org.apache.commons.lang.StringUtils.*;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.services.jobsdao.dao.JobsDao;
import org.systemsbiology.addama.services.jobsdao.pojo.Job;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class HttpJob {
    private static final Logger log = Logger.getLogger(HttpJob.class.getName());

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

    public static Job getJob(JobsDao jobsDao, HttpServletRequest request, String suffix) throws ResourceNotFoundException {
        String jobUri = substringAfter(request.getRequestURI(), request.getContextPath());
        if (!isEmpty(suffix)) {
            jobUri = substringBetween(request.getRequestURI(), request.getContextPath(), suffix);
        }
        log.fine(jobUri);
        Job job = jobsDao.retrieve(jobUri);
        if (job == null) {
            throw new ResourceNotFoundException(jobUri);
        }
        return job;
    }

    public static Job getJobByUri(JobsDao jobsDao, HttpServletRequest request, String requestedUri, String suffix) {
        String jobUri = substringAfter(requestedUri, request.getContextPath());
        if (!isEmpty(suffix)) {
            jobUri = substringBetween(requestedUri, request.getContextPath(), suffix);
        }
        log.fine(jobUri);
        return jobsDao.retrieve(jobUri);
    }

}