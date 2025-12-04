package com.surftools.wimp.practice.tools;

import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class EmailTestTool {
  private static final String EMAIL_FROM = "RTykulsker.eto@gmail.com";
  private static final String EMAIL_TO = "RTykulsker@gmail.com";
  private static String APP_PASSWORD = null;

  public static void main(String[] args) throws Exception {
    if (APP_PASSWORD == null) {
      APP_PASSWORD = args[0];
    }
    System.out.println("pw: " + APP_PASSWORD);
    var message = new MimeMessage(getEmailSession());
    message.setFrom(new InternetAddress(EMAIL_FROM));
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(EMAIL_TO));
    message.setSubject("Email subject");
    message.setText("This is my email sent from Gmail using Java");
    Transport.send(message);
    System.out.println("email sent");
  }

  private static Session getEmailSession() {
    return Session.getInstance(getGmailProperties(), new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(EMAIL_FROM, APP_PASSWORD);
      }
    });
  }

  private static Properties getGmailProperties() {
    Properties prop = new Properties();
    prop.put("mail.smtp.auth", "true");
    prop.put("mail.smtp.starttls.enable", "true");
    prop.put("mail.smtp.host", "smtp.gmail.com");
    prop.put("mail.smtp.port", "587");
    prop.put("mail.smtp.ssl.trust", "smtp.gmail.com");
    return prop;
  }
}
