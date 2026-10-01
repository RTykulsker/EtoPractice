/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

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

package com.surftools.wimp.service.email;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.GenKey;
import com.surftools.wimp.service.IService;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class EmailService implements IService {
	private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

	@Override
	public String getName() {
		return "Email Service";
	}

	public static void sendSimpleEmail(IConfigurationManager cm, String subject, String body) throws Exception {
		String username = cm.getAsString(GenKey.EMAIL_NOTIFICATION_FROM);

		// see myaccount.google.com/apppasswords
		String appPassword = Files.readString(Path.of(cm.getAsString(GenKey.PATH_EMAIL_PASSWORD)));

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, appPassword);
			}
		});

		var from = cm.getAsString(GenKey.EMAIL_NOTIFICATION_FROM);
		var recipients = cm.getAsString(GenKey.EMAIL_NOTIFICATION_TO);

		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
		message.setSubject(subject);
		message.setText(body);

		Transport.send(message);
		logger.info("Email(s) sent to: " + recipients);
	}

}
