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

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.codec.binary.Base32;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.utils.config.IConfigurationKey;
import com.surftools.wimp.utils.config.impl.PropertyFileConfigurationManager;

public class MassMailTool {
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private static final Logger logger = LoggerFactory.getLogger(MassMailTool.class);

  @Option(name = "--configurationFile", usage = "path to configuration file", required = true)
  private String configurationFileName = null;

  public static void main(String[] args) {
    var tool = new MassMailTool();
    CmdLineParser parser = new CmdLineParser(tool);
    try {
      parser.parseArgument(args);
      tool.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  public void run() {
    logger.info("begin massmail");
    try {
      var cm = new PropertyFileConfigurationManager(configurationFileName, Key.values());
      var pathName = cm.getAsString(Key.PATH_NAME);
      var messagePath = Path.of(pathName, cm.getAsString(Key.MESSAGE_FILE_NAME));
      var addressPath = Path.of(pathName, cm.getAsString(Key.ADDRESS_FILE_NAME));
      var outputPath = Path.of(pathName, cm.getAsString(Key.OUTPUT_FILE_NAME));
      var sender = cm.getAsString(Key.SENDER);
      var source = cm.getAsString(Key.SOURCE);
      var subject = cm.getAsString(Key.SUBJECT);

      logger.info("Sender: " + sender);
      logger.info("Source: " + source);
      logger.info("Subject: " + subject);

      var messageContext = new MessageContext(sender, source, subject);

      var messageText = Files.readString(messagePath);
      logger.info("read " + messageText + "from message file: " + messagePath.toString());

      var addresses = Files.readAllLines(addressPath);
      logger.info("read " + addresses.size() + " address lines from address file: " + addressPath.toString());

      var sb = new StringBuilder();
      sb.append(merge(messageContext, messageText, addresses));

      var content = merge(sb.toString());
      Files.writeString(outputPath, content);
      logger.info("wrote " + content.length() + " bytes to output file: " + outputPath.toString());

    } catch (Exception e) {
      logger.error("Exception: " + e.getMessage());
      e.printStackTrace();
    }
    logger.info("end massmail");
  }

  private String merge(String messageList) {
    var sb = new StringBuilder();
    final String header = """
        <?xml version="1.0"?>
        <Winlink_Express_message_export>
          <export_parameters>
            <xml_file_version>1.0</xml_file_version>
            <winlink_express_version>1.7.24.0</winlink_express_version>
          </export_parameters>
          <message_list>
          """;

    final String footer = """
          </message_list>
        </Winlink_Express_message_export>
        """;
    sb.append(header);
    sb.append(messageList);
    sb.append(footer);
    return sb.toString();
  }

  private String merge(MessageContext messageContext, String messageText, List<String> addresses) {

    final String messageTemplate = """
            <message>
              <id>#MESSAGE_ID#</id>
              <foldertype>Fixed</foldertype>
              <folder>Outbox</folder>
              <subject>#SUBJECT#</subject>
              <time>#MESSAGE_TIME#</time>
              <sender>#SENDER#</sender>
              <acknowledged></acknowledged>
              <attachmentsopened></attachmentsopened>
              <replied></replied>
              <rmsoriginator></rmsoriginator>
              <rmsdestination></rmsdestination>
              <rmspath></rmspath>
              <location></location>
              <csize></csize>
              <downloadserver></downloadserver>
              <forwarded></forwarded>
              <messageserver></messageserver>
              <precedence>2</precedence>
              <peertopeer>False</peertopeer>
              <routingflag></routingflag>
              <replied></replied>
              <source>#SOURCE#</source>
              <unread>False</unread>
              <flags>0</flags>
              <messageoptions>False|False||||False|</messageoptions>
              <mime>Date: #MIME_TIME#
        From: #SENDER#@winlink.org
        Reply-To: #SENDER#@winlink.org
        Subject: #SUBJECT#
        To: #TO#
        Message-ID: #MESSAGE_ID#
        X-Source: #SENDER#
        MIME-Version: 1.0

        #BODY#</mime>
            </message>
              """;
    var body = new String(messageText);
    body = body.replaceAll("<", "&lt;");
    body = body.replaceAll("<=", "&lt;=3D");
    body = body.replaceAll(">", "&gt;");
    body = body.replaceAll(">=", "&gt;=3D");

    var now = LocalDateTime.now();
    var sb = new StringBuilder();
    for (var to : addresses) {
      var messageId = generateMid(messageContext.toString() + to);
      var text = messageTemplate;
      text = text.replaceAll("#BODY#", body);
      text = text.replaceAll("#MESSAGE_ID#", messageId);
      text = text.replaceAll("#SENDER#", messageContext.sender);
      text = text.replaceAll("#SOURCE#", messageContext.source);
      text = text.replaceAll("#SUBJECT#", messageContext.subject);
      text = text.replaceAll("#TO#", to);

      DateTimeFormatter MESSAGE_TIME_DTF = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
      text = text.replaceAll("#MESSAGE_TIME#", MESSAGE_TIME_DTF.format(now));

      DateTimeFormatter MIME_DTF = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss +0000");
      text = text.replaceAll("#MIME_TIME#", MIME_DTF.format(now));

      sb.append(text);
      sb.append("\n");
    }

    return sb.toString();
  }

  record MessageContext(String sender, String source, String subject) {
  }

  private String generateMid(String string) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      String stringToHash = string + System.nanoTime();
      md.update(stringToHash.getBytes());
      byte[] digest = md.digest();
      Base32 base32 = new Base32();
      String encodedString = base32.encodeToString(digest);
      String subString = encodedString.substring(0, 12);
      return subString;
    } catch (Exception e) {
      throw new RuntimeException("could not generate messageId: " + e.getMessage());
    }
  }

  enum Key implements IConfigurationKey {
    PATH_NAME("massmail.pathname"), // path to working directory
    MESSAGE_FILE_NAME("massmail.message.filename"), // path to input message file
    ADDRESS_FILE_NAME("massmail.address.filename"), // path to input addresses
    OUTPUT_FILE_NAME("massmail.output.filename"), // path to output Winlink Express import message file
    SENDER("massmail.sender"), // can be tactical address
    SOURCE("massmail.source"), // winlink account holder
    SUBJECT("massmail.subject"), // message subject
    ;

    private final String key;

    private Key(String key) {
      this.key = key;
    }

    public static Key fromString(String string) {
      for (Key key : Key.values()) {
        if (key.toString().equals(string)) {
          return key;
        }
      }
      return null;
    }

    @Override
    public String toString() {
      return key;
    }

  } // end internal class Key
}
