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

package com.surftools.wimp.practice.processors.exercise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Hics259Message;
import com.surftools.wimp.practice.misc.PracticeSummary;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class Hics259PracticeProcessor extends BasePracticeProcessor {
  private final Logger logger = LoggerFactory.getLogger(Hics259PracticeProcessor.class);
  private Hics259Message ref;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    processorMessageType = MessageType.HICS_259;
    ref = (Hics259Message) referenceMessage;
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (Hics259Message) message;

    count(sts.testStartsWith("Message Subject should start with #EV", ref.subject, m.subject));
    count(sts.test("Message Location should be valid", m.msgLocation.isValid(), m.msgLocation.toString()));
    count(sts.test("Incident name should be #EV", ref.incidentName, m.incidentName));
    count(sts.testIfPresent("Form Date should be present", m.formDate));
    count(sts.testIfPresent("Form Time should be present", m.formTime));
    count(sts.test("Operational Period # should be #EV", ref.operationalPeriod, m.operationalPeriod));
    count(sts.test("Operational Date From should be #EV", ref.opFromDate, m.opFromDate));
    count(sts.test("Operational Date To should be #EV", ref.opToDate, m.opToDate));
    count(sts.test("Operational Time From should be #EV", ref.opFromTime, m.opFromTime));
    count(sts.test("Operational Time To should be #EV", ref.opToTime, m.opToTime));

    var refMap = ref.casualtyMap;
    var mMap = m.casualtyMap;
    for (var key : Hics259Message.CASUALTY_KEYS) {
      var refEntry = refMap.get(key);
      var mEntry = mMap.get(key);
      count(sts.test(key + " Adult Count should be #EV", refEntry.adultCount(), mEntry.adultCount()));
      count(sts.test(key + " Pediatric Count should be #EV", refEntry.childCount(), mEntry.childCount()));
      count(sts.test(key + " Comment should be #EV", refEntry.comment(), mEntry.comment()));
    }

    count(sts.test("Patient Tracking Manager should be #EV", ref.patientTrackingManager, m.patientTrackingManager));
    count(sts.test("Facility Name should be #EV", ref.facilityName, m.facilityName));

    var practiceSummary = new PracticeSummary(m, sts);
    practiceSummaries.add(practiceSummary);
  }
}
