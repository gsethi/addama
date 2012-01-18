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
package org.systemsbiology.addama.commons.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StringUtils;

import java.util.ArrayList;

/**
 * @author hrovira
 */
public class Bootstrap {
    public static void main(String[] args) {
        loadApplicationContext(args);
    }

    public static ApplicationContext loadApplicationContext(String... args) {
        ArrayList<String> list = new ArrayList<String>();
        if (args != null) {
            for (String arg : args) {
                if (StringUtils.hasLength(arg)) {
                    list.add(arg);
                }
            }
        }

        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(list.toArray(new String[list.size()]));
        ctx.registerShutdownHook();
        return ctx;
    }
}
