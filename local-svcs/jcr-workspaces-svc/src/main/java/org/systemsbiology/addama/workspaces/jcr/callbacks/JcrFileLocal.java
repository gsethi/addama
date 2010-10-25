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
package org.systemsbiology.addama.workspaces.jcr.callbacks;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springmodules.jcr.JcrTemplate;
import org.springmodules.jcr.SessionFactory;
import org.springmodules.jcr.SessionFactoryUtils;
import org.springmodules.jcr.SessionHolder;
import org.systemsbiology.addama.workspaces.callbacks.FileLocal;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import java.io.*;

/**
 * @author hrovira
 */
public class JcrFileLocal implements FileLocal {
    private final JcrTemplate jcrTemplate;
    private final String localPath;
    private final String jcrPath;

    public JcrFileLocal(JcrTemplate jcrTemplate, String localPath, String jcrPath) {
        this.jcrTemplate = jcrTemplate;
        this.localPath = localPath;
        this.jcrPath = jcrPath;
    }

    public String getLocalFile() throws Exception {
        SessionFactory sf = jcrTemplate.getSessionFactory();
        Session s = SessionFactoryUtils.getSession(sf, true);
        TransactionSynchronizationManager.bindResource(sf, sf.getSessionHolder(s));

        try {
            Node node = (Node) jcrTemplate.getItem(jcrPath);
            Property property = node.getProperty("jcr:data");
            InputStream inputStream = property.getStream();


            File f = new File(localPath);
            f.getParentFile().mkdirs();

            OutputStream outputStream = new FileOutputStream(f);
            byte[] buf = new byte[10000];
            int len;
            while ((len = inputStream.read(buf, 0, 1000)) > 0) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
        } finally {
            SessionHolder sesHolder = (SessionHolder) TransactionSynchronizationManager.unbindResource(sf);
            SessionFactoryUtils.releaseSession(sesHolder.getSession(), sf);
        }

        return localPath;
    }

    public void close() throws IOException {
        new File(localPath).delete();
    }
}