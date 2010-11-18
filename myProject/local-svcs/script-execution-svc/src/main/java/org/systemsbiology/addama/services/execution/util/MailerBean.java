package org.systemsbiology.addama.services.execution.util;

import org.springframework.mail.MailSender;

/**
* @author hrovira
*/
class MailerBean {
    private final MailSender mailSender;
    private final String from;
    private final String subject;
    private final String message;

    MailerBean(MailSender mailSender, String from, String subject, String message) {
        this.mailSender = mailSender;
        this.from = from;
        this.subject = subject;
        this.message = message;
    }

    public MailSender getMailSender() {
        return mailSender;
    }

    public String getFrom() {
        return from;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

}
