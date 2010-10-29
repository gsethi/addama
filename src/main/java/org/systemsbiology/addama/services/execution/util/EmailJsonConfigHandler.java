/*
 * Copyright (C) 2003-2010 Institute for Systems Biology
 *                             Seattle, Washington, USA.
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */
package org.systemsbiology.addama.services.execution.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.systemsbiology.addama.registry.JsonConfigHandler;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * @author hrovira
 */
public class EmailJsonConfigHandler implements JsonConfigHandler {
    private final HashMap<String, MailerBean> mailerBeansByUri = new HashMap<String, MailerBean>();

    public void handle(JSONObject configuration) throws Exception {
        if (configuration.has("locals")) {
            JSONArray locals = configuration.getJSONArray("locals");
            for (int i = 0; i < locals.length(); i++) {
                JSONObject local = locals.getJSONObject(i);
                if (local.has("emailInstructions")) {
                    String uri = local.getString("uri");

                    JSONObject emailInstructions = local.getJSONObject("emailInstructions");

                    String from = emailInstructions.getString("from");
                    String subject = emailInstructions.getString("subject");
                    String message = getMessage(emailInstructions.getString("emailText"));
                    String host = emailInstructions.getString("host");

                    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
                    mailSender.setHost(host);

                    mailerBeansByUri.put(uri, new MailerBean(mailSender, from, subject, message));
                }
            }
        }
    }

    public Mailer getMailer(String uri, String jobLabel, String toAddress) {
        if (mailerBeansByUri.containsKey(uri)) {
            return new Mailer(mailerBeansByUri.get(uri), jobLabel, toAddress);
        }
        return null;
    }

    /*
     * Private Methods
     */

    private String getMessage(String templatePath) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(templatePath)));
        StringBuilder builder = new StringBuilder();
        String line = "";
        while (line != null) {
            line = reader.readLine();
            if (line != null) {
                builder.append(line);
                builder.append("\n");
            }
        }
        return builder.toString();
    }

}
