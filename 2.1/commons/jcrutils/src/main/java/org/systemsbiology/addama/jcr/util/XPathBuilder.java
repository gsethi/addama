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
package org.systemsbiology.addama.jcr.util;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ISO9075;

/**
 * Created by IntelliJ IDEA.
 * User: jlin
 * Date: Mar 26, 2009
 * Time: 11:18:00 AM
 */
public class XPathBuilder {
    public static final String SPACE = " ";
    public static final String XSPACE = "_x0020_";

    public static String getTranslatedXpath(String path) {
        if (StringUtils.isEmpty(path)) return null;
        path = path.replaceAll(XPathBuilder.SPACE, XPathBuilder.XSPACE);

        return path;
    }

    public static String getISO9075XPath(String path) {
        //log.info("Path before ISO9075 encoding:" + path);
        if (StringUtils.isEmpty(path)) return "";
        path = ISO9075.encodePath(path);
        //log.info("Path before ISO9075 encoding:" + path);
        if (StringUtils.contains(path, XPathBuilder.SPACE)) path = XPathBuilder.getTranslatedXpath(path);
        return path;
    }

    public static String getISO9075XKey(String key) {
        if (StringUtils.isEmpty(key)) return null;
        return "@" + ISO9075.encode(key);
    }

}
