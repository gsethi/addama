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
package org.systemsbiology.addama.rest.views;

import org.springframework.web.servlet.View;
import org.systemsbiology.addama.jcr.util.NodeUtil;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * @author hrovira
 */
public class NodeFileView implements View {

    public String getContentType() {
        return null;
    }

    public void render(Map map, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Node node = (Node) map.get("node");

        if (NodeUtil.isFileNode(node)) {
            outputFile(node, node.getName(), response);
        } else {
            outputFile(node.getNode("jcr:content"), node.getName(), response);
        }
    }

    /*
     * Private Methods
     */

    private void outputFile(Node contentNode, String filename, HttpServletResponse response) throws IOException, RepositoryException {
        Property mimeProperty = contentNode.getProperty("jcr:mimeType");
        Property dataProperty = contentNode.getProperty("jcr:data");
        InputStream inputStream = dataProperty.getStream();

        response.setContentType(mimeProperty.getString());
        response.setHeader("Content-Disposition", "filename=\"" + filename + "\"");
        pipe(inputStream, response.getOutputStream());
    }

    private void pipe(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buf = new byte[10000];
        int len;
        while ((len = inputStream.read(buf, 0, 1000)) > 0) {
            outputStream.write(buf, 0, len);
        }
        outputStream.close();
    }
}
