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

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.practice.misc.PracticeSummary;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class Ics213PracticeProcessor extends BasePracticeProcessor {

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, MessageType.ICS_213);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (Ics213Message) message;
    var ref = (Ics213Message) referenceMessage;

    count(sts.testStartsWith("Message Subject should start with #EV", referenceMessage.subject, m.subject));
    count(sts.test("Message Location should be valid", m.msgLocation.isValid(), m.msgLocation.toString()));
    count(sts.test("Form Location should be valid", m.formLocation.isValid(), m.formLocation.toString()));
    count(sts.test("Organization Name should be #EV", ref.organization, m.organization));
    count(sts.test("THIS IS AN EXERCISE should be checked", m.isExercise));
    count(sts.test("Incident Name should be #EV", ref.incidentName, m.incidentName));
    count(sts.test("Form To should be #EV", ref.formTo, m.formTo));
    count(sts.test("Form From should be #EV", ref.formFrom, m.formFrom));
    count(sts.test("Form Subject should be #EV", ref.formSubject, m.formSubject));

    var formDateTime = LocalDateTime.parse(m.formDate + " " + m.formTime, DTF);
    count(sts.testOnOrAfter("Form Date and Time should be on or after #EV", windowOpenDT, formDateTime, DTF));
    count(sts.testOnOrBefore("Form Date and Time should be on or before #EV", windowCloseDT, formDateTime, DTF));

    count(sts.test("Message should be #EV", ref.formMessage, m.formMessage));
    count(sts.test("Approved by should be #EV", ref.approvedBy, m.approvedBy));
    count(sts.test("Position/Title should be #EV", ref.position, m.position));

    var practiceSummary = new PracticeSummary(m, sts);
    practiceSummaries.add(practiceSummary);
  }
}
