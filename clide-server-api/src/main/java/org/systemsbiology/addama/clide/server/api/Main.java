package org.systemsbiology.addama.clide.server.api;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("must include at least one file to upload in arguments");
        }

        ApplicationContext appCtx = new ClassPathXmlApplicationContext(new String[]{"apiKeyHttpClientTemplate.xml", "clideServer.xml"});

        ClideServer clideServer = (ClideServer) appCtx.getBean("clideServer");
        JSONArray clients = clideServer.getClients();
        if (clients != null && clients.length() > 0) {
            for (int i = 0; i < clients.length(); i++) {
                JSONObject client = clients.getJSONObject(i);
                String clientUri = client.getString("uri");

                for (String arg : args) {
                    File f = new File(arg);
                    log.info("\n\tpushing:" + f.getPath() + "\n\tto:" + clientUri + "\n\n");
                    if (f.exists()) {
                        if (f.isDirectory()) {
                            push(clideServer, getUri(clientUri, f), f.listFiles());
                        } else {
                            push(clideServer, getUri(clientUri, f.getParentFile()), f);
                        }
                    }
                }
            }
        }
    }

    private static void push(ClideServer clideServer, String uri, File... files) throws Exception {
        JSONObject response = clideServer.push(uri, files);
        if (response != null) {
            log.info("\nresponse=" + response.toString(4) + "\n");
        } else {
            log.warning("\ninvalid response\n");
        }
    }

    private static String getUri(String uri, File f) {
        String chompUri = StringUtils.chomp(uri, "/");
        String filepath = f.getPath();
        if (filepath.startsWith("/")) {
            return chompUri + "/" + StringUtils.substringAfter(filepath, "/");
        }
        return chompUri + "/" + filepath;
    }
}
