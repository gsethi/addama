package org.systemsbiology.addama.clide.server.api;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;

/**
 * @author hrovira
 */
public class Main {
    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("must include at least one file to upload in arguments");
        }

        ApplicationContext appCtx = new ClassPathXmlApplicationContext(new String[]{"apiKeyHttpClientTemplate.xml", "clideServer.xml"});

        ClideServer clideServer = (ClideServer) appCtx.getBean("clideServer");
        String[] clientUris = clideServer.getClientUris();
        if (clientUris != null && clientUris.length > 0) {
            for (String clientUri : clientUris) {
                for (String arg : args) {
                    File f = new File(arg);
                    if (f.exists()) {
                        if (f.isDirectory()) {
                            clideServer.upload(getUri(clientUri, f), f.listFiles());
                        } else {
                            clideServer.upload(getUri(clientUri, f.getParentFile()), f);
                        }
                    }
                }
            }
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
