package com.example.sendsms;

import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

/**
 * Created by ss on 19/4/14.
 */
class Email {

    public static void send(String content, String email) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.163.com");
        props.put("mail.smtp.port", 25);
        props.put("mail.smtp.auth", true);
        String fromEmail = AppData.getInstance().getProperties(MainActivity.fromEmail, String.class);
        String fromEmailPasswd = AppData.getInstance().getProperties(MainActivity.fromEmailPasswd, String.class);
        Session session = Session.getInstance(props, new MailAuthenticator(fromEmail, fromEmailPasswd));

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(fromEmail);
            msg.setRecipients(Message.RecipientType.TO, email);
            msg.setSubject("短信消息");
            msg.setSentDate(new Date());
            msg.setText(content);
            Transport.send(msg);
            System.out.println("send success");
        } catch (MessagingException mex) {
            System.out.println("send failed, exception: " + mex);
        }
    }

    static class MailAuthenticator extends Authenticator {
        private String userName;
        private String password;

        public MailAuthenticator(String userName, String password) {
            this.userName = userName;
            this.password = password;
        }

        // 这个方法在Authenticator内部会调用
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(userName, password);
        }
    }

    public static void main(String args[]) {
        send("测试", "test12456@qq.com");
    }
}
