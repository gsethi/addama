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

import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class Mailer {
    private static final Logger log = Logger.getLogger(Mailer.class.getName());

    private final MailerBean mailerBean;
    private final String jobLabel;
    private final String toAddress;
    private String jobLog;

    public Mailer(MailerBean mailerBean, String jobLabel, String toAddress) {
        this.mailerBean = mailerBean;
        this.jobLabel = jobLabel;
        this.toAddress = toAddress;
    }

    public void setJobLog(String jobLog) {
        this.jobLog = jobLog;
    }

    public void sendMail() {
        try {
            String from = this.mailerBean.getFrom();
            String subject = this.mailerBean.getSubject();
            if (!StringUtils.isEmpty(jobLabel)) {
                subject = this.mailerBean.getSubject() + " (" + jobLabel + ")";
            }
            String text = this.mailerBean.getMessage();

            JavaMailSenderImpl sender = (JavaMailSenderImpl) mailerBean.getMailSender();

            MimeMessage message = sender.createMimeMessage();

            // use the true flag to indicate you need a multipart message
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(toAddress);
            helper.setSubject(subject);
            helper.setText(text);
            helper.setFrom(from);
            if (!StringUtils.isEmpty(jobLog)) {
                File f = new File(jobLog);
                if (f.exists()) {
                    log.info("attaching :" + f.getPath());
                    helper.addAttachment(f.getName(), new FileSystemResource(f));
                }
            }
            sender.send(message);

            log.info("sent email to " + toAddress);
        }
        catch (Exception ex) {
            log.warning(ex.getMessage());
        }
    }
}
