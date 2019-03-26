package com.datapath.integration.email;

import lombok.extern.slf4j.Slf4j;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Slf4j
public class EmailSender {

    private static final String USER_NAME = "alekseytestingid@gmail.com";
    private static final String PASSWORD = "QW12er#$";
    private static final String RECIPIENT = "aleksey.dubachinskiy@introlab-systems.com";

    public static boolean sendFailedTenderNotification(String tenderId) {
        String subject = "Tender loading failed";
        String body = "Failed to load tender with outer id " + tenderId;
        return sendFromGMail(USER_NAME, PASSWORD, RECIPIENT, subject, body);
    }

    public static boolean sendTenderValidationFailedNotification(String tenderId) {
        String subject = "Tender validation failed";
        String body = "Failed tender validation. Outer id:  " + tenderId;
        return sendFromGMail(USER_NAME, PASSWORD, RECIPIENT, subject, body);
    }

    public static boolean sendTendersLoadingCheckerNotification() {
        String subject = "Tenders loading failed";
        String body = "Tenders loading failed. Please, fix it.";
        return sendFromGMail(USER_NAME, PASSWORD, RECIPIENT, subject, body);
    }

    private static boolean sendFromGMail(String from, String pass, String to, String subject, String body) {
        Properties props = System.getProperties();
        String host = "smtp.gmail.com";
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.user", from);
        props.put("mail.smtp.password", pass);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");

        Session session = Session.getDefaultInstance(props);
        MimeMessage message = new MimeMessage(session);

        try {
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(body);

            Transport transport = session.getTransport("smtp");
            transport.connect(host, from, pass);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            return true;
        } catch (Exception me) {
            log.error(me.getMessage(), me);
            return false;
        }
    }


}
