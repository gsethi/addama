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
package org.systemsbiology.addama.repositories.jcrrepo.register;

import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.systemsbiology.addama.repositories.jcrrepo.JcrConnection;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;

/**
 * @author hrovira
 */
public class JcrConnectionImpl implements JcrConnection {
    private final String rmiConnection;
    private final String username;
    private final char[] password;

    public JcrConnectionImpl(String rmiConnection, String username, String password) {
        this.rmiConnection = rmiConnection;
        this.username = username;
        this.password = password.toCharArray();
    }

    public Repository getRepository() throws Exception {
        ClientRepositoryFactory repositoryFactory = new ClientRepositoryFactory();
        return repositoryFactory.getRepository(rmiConnection);
    }

    public Credentials getCredentials() {
        return new SimpleCredentials(username, password);
    }
}
