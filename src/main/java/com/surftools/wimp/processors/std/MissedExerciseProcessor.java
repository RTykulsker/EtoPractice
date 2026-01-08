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

package com.surftools.wimp.processors.std;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.persistence.PersistenceManager;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.JoinedUser;
import com.surftools.wimp.persistence.dto.ReturnStatus;
import com.surftools.wimp.practice.tools.PracticeProcessorTool;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * compose outbound messages to folks who missed latest exercise
 */
public class MissedExerciseProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(MissedExerciseProcessor.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {

  }

  @Override
  public void process() {
  }

  @Override
  public void postProcess() {
    var db = new PersistenceManager(cm);
    var ret = db.getHealth();
    if (ret.status() != ReturnStatus.OK) {
      logger.error("Health Check failed: " + ret.content());
      return;
    }

    ret = db.getFilteredExercises(null, date);
    if (ret.status() != ReturnStatus.OK) {
      logger.error("Could not get filteredExercises database: " + ret.content());
      return;
    }
    @SuppressWarnings("unchecked")
    var filteredExercises = (List<Exercise>) ret.data();

    var missLimit = cm.getAsInt(Key.PERSISTENCE_MISS_LIMIT, 3);
    ret = db.getUsersMissingExercises(filteredExercises, missLimit);
    if (ret.status() != ReturnStatus.OK) {
      logger.error("Could not get missed Exercise records from database: " + ret.content());
      return;
    }

    @SuppressWarnings("unchecked")
    List<JoinedUser> joinedUsers = (List<JoinedUser>) ret.data();
    if (joinedUsers.size() == 0) {
      logger.warn("no Users with missed exercises");
      return;
    }

    var template = DEFAULT_TEMPLATE;
    var templatePath = cm.getAsString(Key.PERSISTENCE_MISS_BODY_PATH);
    if (templatePath != null) {
      try {
        template = Files.readString(Path.of(templatePath));
      } catch (Exception e) {
        logger.error("Exception reading template: " + templatePath + ". using default");
      }
    }

    var dateString = cm.getAsString(Key.EXERCISE_DATE);
    var outboundMessages = new ArrayList<OutboundMessage>();
    var from = cm.getAsString(Key.OUTBOUND_MESSAGE_SENDER);
    var subject = cm.getAsString(Key.PERSISTENCE_MISS_SUBJECT, "We missed you!");
    var instructions = (String) mm.getContextObject(PracticeProcessorTool.INSTRUCTIONS_KEY);
    for (var joinedUser : joinedUsers) {
      var to = joinedUser.user.call();
      // inside loop, because we might want to show name, count of missed exercises, etc.
      template = template.replace("#DATE#", dateString);
      template = template.replace("#INSTRUCTIONS#", instructions);
      var body = template;
      var outboundMessage = new OutboundMessage(from, to, subject, body, null);
      outboundMessages.add(outboundMessage);
    }

    var filename = "Winlink_MissedExercise_ImportMesages-" + dateString + ".xml";
    var service = new OutboundMessageService(cm, mm, null, filename);
    service.sendAll(outboundMessages);
  }

  private static String DEFAULT_TEMPLATE = """
      We noticed that you missed the last regularly scheduled ETO Practice exercise on #DATE#.

      Here are the instructions for the next ETO Practice exercise.
      #INSTRUCTIONS#

      We look forward to seeing your Winlink message addressed to ETO-PRACTICE!
            """;
}
