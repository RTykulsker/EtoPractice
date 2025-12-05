/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


*/

package com.surftools.wimp.practice.processors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * send email to various ETO members to notify that weekly practice processing
 * is complete
 */
public class EmailNotificationProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(EmailNotificationProcessor.class);
  private String dateString = null;

  private String from;
  private InternetAddress[] recipientAddresses;
  private String password;
  private String subject;
  private String body;

  private Session session = null;

  private boolean isInitialized = true;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    var enabled = cm.getAsBoolean(Key.PRACTICE_ENABLE_FINALIZE);
    if (!enabled) {
      logger.info("NO email notifications, because not enabled via command line");
      isInitialized = false;
    }

    dateString = cm.getAsString(Key.EXERCISE_DATE);

    from = cm.getAsString(Key.EMAIL_NOTIFICATION_FROM);
    if (from == null || from.isBlank()) {
      logger.info("NO email notifications, because from is null");
    }

    var toListString = cm.getAsString(Key.EMAIL_NOTIFICATION_TO);
    if (toListString == null || toListString.isBlank()) {
      logger.info("NO email notifications, because to is null");
      isInitialized = false;
    }
    var fields = toListString.split(",");
    recipientAddresses = new InternetAddress[fields.length];
    for (var i = 0; i < fields.length; ++i) {
      try {
        recipientAddresses[i] = new InternetAddress(fields[i]);
      } catch (AddressException e) {
        logger.error("Exception parsing adddress[" + i + "] (" + fields[i] + "): " + e.getMessage());
        isInitialized = false;
      }
    }

    var passwordFilePath = cm.getAsString(Key.EMAIL_NOTIFICATION_PASSWORD_FILEPATH);
    if (passwordFilePath == null || passwordFilePath.isBlank()) {
      logger.info("NO email notifications, because password.filePath is null");
      isInitialized = false;
    }

    try {
      password = Files.readString(Path.of(passwordFilePath));
    } catch (IOException e) {
      logger.error("Exception reading password filePath:" + passwordFilePath + ", " + e.getMessage());
      isInitialized = false;
    }

    if (!isInitialized) {
      return;
    }

    subject = cm.getAsString(Key.EMAIL_NOTIFICATION_SUBJECT, "ETO: WLT processings is complete for #DATE#");
    subject = subject.replaceAll("#DATE#", dateString);

    body = cm.getAsString(Key.EMAIL_NOTIFICATION_BODY, "Ready for you to send groups.io message to all");
    body = body.replaceAll("#DATE#", dateString);

    Properties prop = new Properties();
    prop.put("mail.smtp.auth", "true");
    prop.put("mail.smtp.starttls.enable", "true");
    prop.put("mail.smtp.host", "smtp.gmail.com");
    prop.put("mail.smtp.port", "587");
    prop.put("mail.smtp.ssl.trust", "smtp.gmail.com");

    try {
      session = Session.getInstance(prop, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(from, password);
        }
      });
    } catch (Exception e) {
      logger.error("Exception creating session: " + e.getLocalizedMessage());
      isInitialized = false;
      session = null;
    }

    if (isInitialized) {
      logger.info("Email notification from: " + from);
      logger.info("Email notification to: " + toListString);
      logger.info("Email notification subject: " + subject);
      logger.info("Email notification body: " + body);
    }
  }

  @Override
  public void process() {
  }

  @Override
  public void postProcess() {
    if (!isInitialized) {
      logger.info("NO email notifications");
      return;
    }

    try {
      var message = new MimeMessage(session);
      message.setFrom(new InternetAddress(from));
      message.setRecipients(Message.RecipientType.TO, recipientAddresses);
      message.setSubject(subject);
      message.setText(body);
      Transport.send(message);
      logger.info("email sent");
    } catch (Exception e) {
      logger.error("Exception sending notification message: " + e.getMessage());
    }

  } // end postProcess

}
