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
package org.systemsbiology.addama.services.execution.notification;

import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.systemsbiology.addama.services.execution.jobs.Job;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.systemsbiology.addama.services.execution.jobs.JobStatus.completed;

/**
 * @author hrovira
 */
public class EmailNotifier implements JobNotifier {
    private static final Logger log = Logger.getLogger(EmailNotifier.class.getName());

    private final EmailBean emailBean;

    public EmailNotifier(EmailBean emailBean) {
        this.emailBean = emailBean;
    }

    public void notify(Job job) {
        try {
            if (this.emailBean == null) {
                log.warning("no email configuration found");
                return;
            }

            if (isEmpty(job.getEmail())) {
                log.warning("no email address for job: " + job.getJobUri());
                return;
            }

            if (job.getJobStatus().equals(completed)) {
                JavaMailSenderImpl sender = (JavaMailSenderImpl) emailBean.getMailSender();
                MimeMessage message = sender.createMimeMessage();

                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                helper.setTo(job.getEmail());
                helper.setSubject(createSubject(job));
                helper.setText(this.emailBean.getMessage());
                helper.setFrom(this.emailBean.getFrom());
                attachLogs(job, helper);
                sender.send(message);

                log.fine("sent email to " + job.getEmail());
            }
        }
        catch (Exception ex) {
            log.warning(ex.getMessage());
        }
    }

    private String createSubject(Job job) {
        if (!isEmpty(job.getLabel())) {
            return this.emailBean.getSubject() + " (" + job.getLabel() + ")";
        }
        return this.emailBean.getSubject();
    }

    private void attachLogs(Job job, MimeMessageHelper helper) throws MessagingException {
        if (!isEmpty(job.getLogPath())) {
            File f = new File(job.getLogPath());
            if (f.exists()) {
                log.fine("attaching :" + f.getPath());
                helper.addAttachment(f.getName(), new FileSystemResource(f));
            }
        }
    }
}
