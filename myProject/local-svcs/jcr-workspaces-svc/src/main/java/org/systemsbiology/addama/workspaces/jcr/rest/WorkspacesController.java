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
package org.systemsbiology.addama.workspaces.jcr.rest;

import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.InputStreamFileView;
import org.systemsbiology.addama.workspaces.rest.BaseWorkspacesController;

import javax.jcr.Node;
import javax.jcr.Property;
import java.io.InputStream;

/**
 * @author hrovira
 */
public class WorkspacesController extends BaseWorkspacesController {

    protected ModelAndView getFile(Node node) throws Exception {
        Property dataProperty = node.getProperty("jcr:data");

        String filename = node.getName();
        ModelAndView mav = new ModelAndView(new InputStreamFileView());
        mav.addObject("inputStream", dataProperty.getStream());
        mav.addObject("mimeType", super.getServletContext().getMimeType(filename));
        mav.addObject("filename", filename);
        return mav;
    }

    protected void storeFile(Node node, String filename, InputStream inputStream) throws Exception {
        if (node.hasNode(filename)) {
            Node fileNode = node.getNode(filename);
            fileNode.setProperty("jcr:data", inputStream);
        } else {
            Node fileNode = node.addNode(filename);
            fileNode.addMixin("mix:referenceable");
            fileNode.setProperty("jcr:mimeType", super.getServletContext().getMimeType(filename));
            fileNode.setProperty("jcr:data", inputStream);
        }
    }
}
