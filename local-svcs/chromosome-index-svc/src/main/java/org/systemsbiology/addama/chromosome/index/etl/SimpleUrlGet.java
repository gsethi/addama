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
package org.systemsbiology.addama.chromosome.index.etl;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import static org.apache.commons.lang.StringUtils.chomp;

/**
 * @author hrovira
 */
public class SimpleUrlGet {
    public static void main(String[] args) throws Exception {
        Properties keys = apiKeys(args);

        doGet(keys, "/addama/chromosomes");
        doGet(keys, "/addama/chromosomes/mm9");
        doGet(keys, "/addama/chromosomes/mm9/chr5");
        doGet(keys, "/addama/chromosomes/mm9/chr5/151003000/151003100/sequence");
    }

    private static Properties apiKeys(String[] args) throws IOException {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("must pass apikey file for addama registry");
        }

        Properties p = new Properties();
        p.load(new FileInputStream(args[0]));
        assert (p.containsKey("apikey"));
        assert (p.containsKey("host"));
        return p;
    }

    private static void doGet(Properties properties, String uri) throws Exception {
        URL url = new URL("https://" + chomp(properties.getProperty("host"), "/") + uri);
        URLConnection uc = url.openConnection();
        uc.setRequestProperty("x-addama-apikey", properties.getProperty("apikey"));
        uc.connect();

        System.out.println(uri + ":" + getContent(uc.getInputStream()));
    }

    private static String getContent(InputStream inputStream) throws Exception {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line = "";
        while (line != null) {
            line = reader.readLine();
            if (line != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }
}
