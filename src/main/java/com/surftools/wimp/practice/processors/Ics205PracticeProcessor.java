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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics205Message;
import com.surftools.wimp.practice.misc.PracticeSummary;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class Ics205PracticeProcessor extends BasePracticeProcessor {
  private final Logger logger = LoggerFactory.getLogger(Ics205PracticeProcessor.class);
  private Ics205Message ref;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    processorMessageType = MessageType.ICS_205;
    ref = (referenceMessage instanceof Ics205Message) ? (Ics205Message) referenceMessage : null;
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (Ics205Message) message;

    count(sts.testStartsWith("Message Subject should start with #EV", referenceMessage.subject, m.subject));
    count(sts.test("Message Location should be valid", m.msgLocation.isValid(), m.msgLocation.toString()));

    count(sts.test("Organization Name should be #EV", ref.organization, m.organization));
    count(sts.test("Incident Name should be #EV", ref.incidentName, m.incidentName));

    final var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    var dateTimePrepared = LocalDateTime.parse(m.dateTimePrepared, dtf);
    count(sts.testOnOrAfter("Date/Time prepared should be on or after #EV", windowOpenDT, dateTimePrepared, dtf));
    count(sts.testOnOrBefore("Date/Time prepared should be on or before #EV", windowCloseDT, dateTimePrepared, dtf));
    count(sts.test("Op Period Date From should be #EV", ref.dateFrom, m.dateFrom));
    count(sts.test("Op Period Date To should be #EV", ref.dateTo, m.dateTo));
    count(sts.test("Op Period Time From should be #EV", ref.timeFrom, m.timeFrom));
    count(sts.test("Op Period Time To should be #EV", ref.timeTo, m.timeTo));

    count(sts.test("Special Intructions should be #EV", ref.specialInstructions, m.specialInstructions));
    count(sts.test("Approved by should be #EV", ref.approvedBy, m.approvedBy));

    var maxRadioEntries = Math.min(ref.radioEntries.size(), m.radioEntries.size());
    if (m.radioEntries.size() != ref.radioEntries.size()) {
      logger.warn("### from: " + m.from + ",mId: " + m.messageId + ", m.radioEntries: " + m.radioEntries.size()
          + ", ref.radioEntries: " + ref.radioEntries.size());
    }
    for (var i = 0; i < maxRadioEntries; ++i) {
      var lineNumber = i + 1;
      sts.setExplanationPrefix("  channel use (line " + lineNumber + ") ");
      var entry = m.radioEntries.get(i);
      var refEntry = ref.radioEntries.get(i);
      if (refEntry.isEmpty()) {
        count(sts.testIfEmpty("Zone/Group should be empty", entry.zoneGroup()));
        count(sts.testIfEmpty("Channel # should be empty", entry.channelNumber()));
        count(sts.testIfEmpty("Function be empty", entry.function()));
        count(sts.testIfEmpty("Channel Name should be empty", entry.channelName()));
        count(sts.testIfEmpty("Assignment should be empty", entry.assignment()));
        count(sts.testIfEmpty("RX Freq should be empty", entry.rxFrequency()));
        count(sts.testIfEmpty("RX N or W should be empty", entry.rxNarrowWide()));
        count(sts.testIfEmpty("RX Tone should be empty", entry.rxTone()));
        count(sts.testIfEmpty("TX Freq should be empty", entry.txFrequency()));
        count(sts.testIfEmpty("TX N or W should be empty", entry.txNarrowWide()));
        count(sts.testIfEmpty("TX Tone should be empty", entry.txTone()));
        count(sts.testIfEmpty("Mode should be empty", entry.mode()));
        count(sts.testIfEmpty("Remarks should be empty", entry.remarks()));
      } else {
        count(sts.testIfEmpty("Zone/Group should be empty", entry.zoneGroup()));
        count(sts.test("Channel # should be #EV", refEntry.channelNumber(), entry.channelNumber()));
        count(sts.test("Function should be #EV", refEntry.function(), entry.function()));
        count(sts.test("Channel Name should be #EV", refEntry.channelName(), entry.channelName()));
        count(sts.test("Assignment should be #EV", refEntry.assignment(), entry.assignment()));
        count(sts.testDouble("RX Freq should be #EV", refEntry.rxFrequency(), entry.rxFrequency()));

        if (refEntry.rxNarrowWide().isEmpty()) {
          count(sts.testIfEmpty("RX N or W should be empty", entry.rxNarrowWide()));
        } else {
          count(sts.test("RX N or W should be #EV", refEntry.rxNarrowWide(), entry.rxNarrowWide()));
        }

        if (refEntry.rxTone().isEmpty()) {
          count(sts.testIfEmpty("RX Tone should be empty", entry.rxTone()));
        } else {
          count(sts.testDouble("RX Tone should be #EV", refEntry.rxTone(), entry.rxTone()));
        }

        if (refEntry.txFrequency().isEmpty()) {
          count(sts.testIfEmpty("TX Freq should be empty", entry.txFrequency()));
        } else {
          count(sts.testDouble("TX Freq should be #EV", refEntry.txFrequency(), entry.txFrequency()));
        }

        if (refEntry.txNarrowWide().isEmpty()) {
          count(sts.testIfEmpty("TX N or W should be empty", entry.txNarrowWide()));
        } else {
          count(sts.test("TX N or W should be #EV", refEntry.txNarrowWide(), entry.txNarrowWide()));
        }

        if (refEntry.txTone().isEmpty()) {
          count(sts.testIfEmpty("TX Tone should be empty", entry.txTone()));
        } else {
          count(sts.testDouble("TX Tone should be #EV", refEntry.txTone(), entry.txTone()));
        }

        count(sts.test("Mode should be #EV", refEntry.mode(), entry.mode()));
        count(sts.test("Remarks should be #EV", refEntry.remarks(), entry.remarks()));
      }
    }

    sts.setExplanationPrefix("");
    var dateTimeApproved = LocalDateTime.parse(m.approvedDateTime, dtf);
    count(sts.testOnOrAfter("Date/Time approved should be on or after #EV", windowOpenDT, dateTimeApproved, dtf));
    count(sts.testOnOrBefore("Date/Time approved should be on or before #EV", windowCloseDT, dateTimeApproved, dtf));
    count(sts.test("IAP Page should be #EV", ref.iapPage, m.iapPage));
    var practiceSummary = new PracticeSummary(m, sts);
    practiceSummaries.add(practiceSummary);
  }
}
