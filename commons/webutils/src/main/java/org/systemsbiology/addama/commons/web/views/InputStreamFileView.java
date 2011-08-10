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
package org.systemsbiology.addama.commons.web.views;

import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

/**
 * @author hrovira
 */
public class InputStreamFileView implements View {
    public static final String INPUTSTREAM = "inputStream";
    public static final String MIMETYPE = "mimeType";
    public static final String FILENAME = "filename";

    public String getContentType() {
        return null;
    }

    public void render(Map map, HttpServletRequest request, HttpServletResponse response) throws Exception {
        InputStream inputStream = (InputStream) map.get(INPUTSTREAM);
        if (inputStream == null) {
            response.setStatus(SC_NOT_FOUND);
            return;
        }

        String mimeType = (String) map.get(MIMETYPE);
        String filename = (String) map.get(FILENAME);

        response.setContentType(mimeType);
        response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");

        OutputStream outputStream = response.getOutputStream();

        byte[] buf = new byte[10000];
        int len;
        while ((len = inputStream.read(buf, 0, 1000)) > 0) {
            outputStream.write(buf, 0, len);
        }
        outputStream.close();
    }
}