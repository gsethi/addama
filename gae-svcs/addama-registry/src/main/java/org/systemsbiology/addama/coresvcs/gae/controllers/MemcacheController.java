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
package org.systemsbiology.addama.coresvcs.gae.controllers;

import com.google.appengine.api.memcache.MemcacheServiceFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class MemcacheController extends AbstractController {
    private static final Logger log = Logger.getLogger(MemcacheController.class.getName());

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = request.getRequestURI();
        log.info("handleRequestInternal(" + requestUri + ")");

        String cachedUri = StringUtils.substringAfter(requestUri, "/memcache/clear");
        if (!StringUtils.isEmpty(cachedUri)) {
            log.info("handleRequestInternal(" + requestUri + "): clearing " + cachedUri);
            MemcacheServiceFactory.getMemcacheService().delete(cachedUri);
        } else {
            log.info("handleRequestInternal(" + requestUri + "): clearing all");
            MemcacheServiceFactory.getMemcacheService().clearAll();
        }
        return null;
    }
}