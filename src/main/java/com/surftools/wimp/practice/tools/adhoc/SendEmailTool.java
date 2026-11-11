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

package com.surftools.wimp.practice.tools.adhoc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class SendEmailTool {
	@Option(name = "--smtpHost", usage = "SMTP host name", required = false)
	private String smtpHost = "smtp.gmail.com";

	@Option(name = "--smtpPort", usage = "SMTP port number", required = false)
	private String smtpPort = "587";

	@Option(name = "--username", usage = "username, example=me@gmail.com", required = true)
	private String username = null;

	@Option(name = "--password", usage = "password, example=xyzzy", required = false)
	private String password = null;

	@Option(name = "--passwordFileName", usage = "passwordFileName, see: myaccount.google.com/apppasswords", required = false)
	private String passwordFileName = null;

	@Option(name = "--to", usage = "to addressees, comma-separated", required = false)
	private String to = null;

	@Option(name = "--cc", usage = "cc addressees, comma-separated", required = false)
	private String cc = null;

	@Option(name = "--bcc", usage = "bcc addressees, comma-separated", required = false)
	private String bcc = null;

	@Option(name = "--subject", usage = "subject", required = false)
	private String subject = "email test";

	@Option(name = "--body", usage = "body text", required = false)
	private String body = "email test";

	@Option(name = "--attachments", usage = "attachment file names, comma-separated", required = false)
	private String attachments = null;

	public static void main(String[] args) {
		var tool = new SendEmailTool();
		CmdLineParser parser = new CmdLineParser(tool);
		try {
			parser.parseArgument(args);
			tool.run();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			parser.printUsage(System.err);
		}
	}

	public void run() throws Exception {
		if (password == null && passwordFileName == null) {
			throw new IllegalArgumentException("Either password or passwordFileName must be specified");
		}

		if (password != null && passwordFileName != null) {
			throw new IllegalArgumentException("Only one of password or passwordFileName can be specified");
		}

		// see myaccount.google.com/apppasswords
		if (password == null) {
			password = Files.readString(Path.of(passwordFileName));
		}

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", smtpHost);
		props.put("mail.smtp.port", smtpPort);

		Session session = Session.getInstance(props, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(username));

		if (to == null) {
			to = username;
		}
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

		if (cc != null) {
			message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
		}

		if (bcc != null) {
			message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc));
		}

		message.setSubject(subject);

		if (attachments == null) {
			message.setText(body);
		} else {
			var multipart = new MimeMultipart();
			var bodyPart = new MimeBodyPart();
			bodyPart.setText(body);
			multipart.addBodyPart(bodyPart);

			var attachmentsArray = attachments.split(",");
			for (var attachmentName : attachmentsArray) {
				var attachmentPart = new MimeBodyPart();

				var mimeType = getMimeType(attachmentName);
				var isString = isMimeTypeString(mimeType);
				byte[] bytes = null;
				if (isString) {
					var string = Files.readString(Path.of(attachmentName));
					bytes = string.getBytes(StandardCharsets.UTF_8);
				} else {
					bytes = Files.readAllBytes(Path.of(attachmentName));
				}

				var dataSource = new ByteArrayDataSource(bytes, mimeType);
				attachmentPart.setDataHandler(new DataHandler(dataSource));
				attachmentPart.setFileName(attachmentName);
				multipart.addBodyPart(attachmentPart);
			}
			message.setContent(multipart);
		}

		Transport.send(message);
		System.out.println("sent email!");
	}

	private String getMimeType(String fileName) {
		try {
			String mime = Files.probeContentType(Path.of(fileName));
			if (mime != null) {
				return mime;
			}
		} catch (Exception ignored) {
		}

		// fallback to extension map
		return getMimeTypeFromExtension(fileName);
	}

	private boolean isMimeTypeString(String mimeType) {
		if (mimeType.startsWith("text")) {
			return true;
		}

		if (mimeType.startsWith("application")) {
			if (mimeType.endsWith("javascript") || mimeType.endsWith("json") || mimeType.endsWith("xml")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return a best-guess MIME type based on filename extension.
	 */
	private String getMimeTypeFromExtension(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return "application/octet-stream";
		}

		int dot = fileName.lastIndexOf('.');
		if (dot < 0 || dot == fileName.length() - 1) {
			return "application/octet-stream";
		}

		String extension = fileName.substring(dot + 1).toLowerCase();
		return MIME_TYPES.getOrDefault(extension, "application/octet-stream");
	}

	private static final Map<String, String> MIME_TYPES = new HashMap<>();

	static {
		MIME_TYPES.put("txt", "text/plain");
		MIME_TYPES.put("html", "text/html");
		MIME_TYPES.put("htm", "text/html");
		MIME_TYPES.put("css", "text/css");
		MIME_TYPES.put("js", "application/javascript");
		MIME_TYPES.put("json", "application/json");
		MIME_TYPES.put("xml", "application/xml");

		MIME_TYPES.put("pdf", "application/pdf");

		MIME_TYPES.put("jpg", "image/jpeg");
		MIME_TYPES.put("jpeg", "image/jpeg");
		MIME_TYPES.put("png", "image/png");
		MIME_TYPES.put("gif", "image/gif");
		MIME_TYPES.put("svg", "image/svg+xml");

		MIME_TYPES.put("zip", "application/zip");

		MIME_TYPES.put("csv", "text/csv");

		MIME_TYPES.put("mp3", "audio/mpeg");
		MIME_TYPES.put("wav", "audio/wav");

		MIME_TYPES.put("mp4", "video/mp4");

		MIME_TYPES.put("doc", "application/msword");
		MIME_TYPES.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

		MIME_TYPES.put("xls", "application/vnd.ms-excel");
		MIME_TYPES.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}
}
