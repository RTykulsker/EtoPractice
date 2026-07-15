/**

The MIT License (MIT)

Copyright (c) 2022, Robert Tykulsker

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

package com.surftools.wimp.processors.std;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IParser;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * classify message by type
 *
 * @author bobt
 *
 */
public class ClassifierProcessor extends AbstractBaseProcessor {

  public final static List<MessageType> IGNORED_TYPES = List.of(MessageType.EXPORTED, MessageType.REJECTS);

  private static final Logger logger = LoggerFactory.getLogger(ClassifierProcessor.class);

  private Map<MessageType, IParser> parserMap = new HashMap<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm);

    for (var type : MessageType.values()) {
      if (IGNORED_TYPES.contains(type)) {
        continue;
      }
      var parserName = "com.surftools.wimp.parser." + type.makeParserName() + "Parser";
      try {
        var parserClass = Class.forName(parserName);
        var parser = (IParser) parserClass.getDeclaredConstructor().newInstance();
        parser.initialize(cm, mm);
        parserMap.put(type, parser);
      } catch (Exception e) {
        logger.error("Couldn't create parser for: " + type.toString() + ", " + e.getLocalizedMessage());
      }
    }
  }

  @Override
  public void process() {
    var messages = mm.getOriginalMessages();

    if (messages != null) {
      var tmpMessageMap = new HashMap<MessageType, List<ExportedMessage>>();

      for (var message : messages) {
        var messageType = findMessageType(message);
        var parser = parserMap.get(messageType);
        ExportedMessage parsedMessage = message;
        if (parser != null) {
          parsedMessage = parser.parse(message);
        }

        var parsedMessageType = parsedMessage.getMessageType();
        var list = tmpMessageMap.getOrDefault(parsedMessageType, new ArrayList<ExportedMessage>());
        list.add(parsedMessage);
        tmpMessageMap.put(parsedMessageType, list);

      } // end loop over messages

      mm.load(tmpMessageMap);
    }
  }

  /**
   * determine the messageType of the ExportedMessage
   *
   * this is the "heart and soul" of classification
   *
   * @param message
   * @return
   */
  public MessageType findMessageType(ExportedMessage message) {
    // First choice: for source-of-truth is the RMS viewer (aka XML blob) attachment
    var messageType = getMessageTypeFromRmsViewerData(message);
    if (messageType != null) {
      return messageType;
    }

    // default
    return MessageType.PLAIN;
  }

  private MessageType getMessageTypeFromRmsViewerData(ExportedMessage message) {
    var attachments = message.attachments;
    if (attachments == null || attachments.size() == 0) {
      return null;
    }

    for (var attachmentName : attachments.keySet()) {
      if (attachmentName == null) {
        continue;
      }

      for (var messageType : MessageType.values()) {
        if (messageType.rmsViewerName() == null) {
          continue;
        }

        if (attachmentName.startsWith(messageType.rmsViewerName())) {
          return messageType;
        }

        // I don't know how, but somehow attachment name is not as expected ...
        if (attachmentName.startsWith(messageType.rmsViewerName().replaceAll(" ", "_"))) {
          // oh what a tangled web we weave ...
          var bytes = message.attachments.get(attachmentName);
          message.attachments.remove(attachmentName);
          message.attachments.put(messageType.rmsViewerName(), bytes);
          return messageType;
        }

      } // end loop over messageTypes
    } // end loop over attachment names

    return null;
  }

}
