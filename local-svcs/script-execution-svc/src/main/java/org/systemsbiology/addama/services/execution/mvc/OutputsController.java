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
package org.systemsbiology.addama.services.execution.mvc;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class OutputsController extends BaseController implements ServletContextAware {
    private static final Logger log = Logger.getLogger(OutputsController.class.getName());

    private ServletContext servletContext;
    private int bufferSize = 8096;
    private String jobPath;

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public void setJobPath(String jobPath) {
        this.jobPath = jobPath;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @RequestMapping(value = "/**/outputs/**", method = RequestMethod.GET)
    public ModelAndView getJobOutput(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info(request.getRequestURI());

        String scriptUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), jobPath);
        String workDir = workDirsByUri.get(scriptUri);
        if (StringUtils.isEmpty(workDir)) {
            throw new ResourceNotFoundException("work directory for " + scriptUri);
        }

        String outputFilePath = StringUtils.substringAfter(request.getRequestURI(), jobPath);
        String filepath = workDir + jobPath + outputFilePath;

        log.info("filepath=" + filepath);
        File outputDir = new File(filepath);
        if (!outputDir.isDirectory()) {
            String filename = StringUtils.substringAfterLast(filepath, "/");
            response.setContentType(servletContext.getMimeType(filename));

            InputStream inputStream = new FileInputStream(filepath);
            OutputStream outputStream = response.getOutputStream();

            byte[] buffer = new byte[this.bufferSize];
            while (true) {
                int bytesRead = inputStream.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                outputStream.write(buffer, 0, bytesRead);
            }
            return null;
        }

        JSONObject json = new JSONObject();

        String baseUri = StringUtils.substringAfterLast(request.getRequestURI(), request.getContextPath());
        if (outputDir.isDirectory()) {
            for (File f : getOutputFiles(outputDir)) {
                String uri = baseUri + StringUtils.substringAfterLast(f.getPath(), filepath);
                json.append("items", new JSONObject().put("uri", uri).put("name", f.getName()));
            }
        }

        ModelAndView mav = new ModelAndView(new JsonItemsView());
        mav.addObject("json", json);
        return mav;
    }

}