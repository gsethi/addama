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
package org.systemsbiology.addama.jcr.support;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springmodules.jcr.JcrTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class JcrTemplateSaveInterceptor extends HandlerInterceptorAdapter {
    private static final Logger log = Logger.getLogger(JcrTemplateSaveInterceptor.class.getName());

    public void postHandle(HttpServletRequest request, HttpServletResponse response, final Object handler, ModelAndView mav) throws Exception {
        try {
            // TODO : This may not be needed...
            ReflectionUtils.doWithFields(handler.getClass(), new ReflectionUtils.FieldCallback() {
                public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                    ReflectionUtils.makeAccessible(field);

                    JcrTemplate jcrTemplate = (JcrTemplate) field.get(handler);
                    if (jcrTemplate != null) {
                        boolean hasPendingChanges = jcrTemplate.hasPendingChanges();
                        if (hasPendingChanges) {
                            log.info("saving");
                            jcrTemplate.save();
                        }
                    }
                }
            }, new ReflectionUtils.FieldFilter() {
                public boolean matches(Field field) {
                    return StringUtils.equals(field.getName(), "jcrTemplate");
                }
            });
        } catch (Exception ex) {
            log.warning("Handler does not contain jcrTemplate: [" + handler.getClass() + "]:" + ex);
        } finally {
            super.postHandle(request, response, handler, mav);
        }
    }
}
