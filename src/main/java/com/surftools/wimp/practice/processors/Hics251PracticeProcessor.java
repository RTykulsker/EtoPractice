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

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Hics251Message;
import com.surftools.wimp.practice.misc.PracticeSummary;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class Hics251PracticeProcessor extends BasePracticeProcessor {
  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, MessageType.HICS_251);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (Hics251Message) message;
    var ref = (Hics251Message) referenceMessage;

    count(sts.testStartsWith("Message Subject should start with #EV", ref.subject, m.subject));
    count(sts.test("Message Location should be valid", m.msgLocation.isValid(), m.msgLocation.toString()));
    count(sts.test("Incident name should be #EV", ref.incidentName, m.incidentName));
    count(sts.test("Page Number should be #EV", ref.pageNumber, m.pageNumber));
    count(sts.test("Page Total should be #EV", ref.pageTotal, m.pageTotal));

    count(sts.test("Operational Period # should be #EV", ref.operationalPeriod, m.operationalPeriod));
    count(sts.test("Operational Date From should be #EV", ref.opFromDate, m.opFromDate));
    count(sts.test("Operational Date To should be #EV", ref.opToDate, m.opToDate));
    count(sts.test("Operational Time From should be #EV", ref.opFromTime, m.opFromTime));
    count(sts.test("Operational Time To should be #EV", ref.opToTime, m.opToTime));

    count(sts.test("Department Name should be #EV", ref.departmentName, m.departmentName));
    count(sts.test("Contact Number should be #EV", ref.contactNumber, m.contactNumber));
    count(sts.test("Street Address should be #EV", ref.streetAddress, m.streetAddress));
    count(sts.test("City should be #EV", ref.city, m.city));
    count(sts.test("State should be #EV", ref.state, m.state));
    count(sts.test("Zip should be #EV", ref.zip, m.zip));

    var refStatusEntryMap = ref.statusEntryMap;
    var mStatusEntryMap = m.statusEntryMap;
    for (var systemName : Hics251Message.SYSTEM_NAMES) {
      var refEntry = refStatusEntryMap.get(systemName);
      var mEntry = mStatusEntryMap.get(systemName);
      count(sts.test(systemName + " Status should be #EV", refEntry.status().toString(), mEntry.status().toString()));
      count(sts.test_2line(systemName + " Comments should be #EV", refEntry.comments(), mEntry.comments()));
    }

    count(sts.test_2line("Remarks should be #EV", ref.remarks, m.remarks));
    count(sts.test("Prepared By should be #EV", ref.preparedBy, m.preparedBy));
    count(sts.testIfPresent("Form Date/Time should be present", m.formDateTime));
    count(sts.test("Facility Name should be #EV", ref.facilityName, m.facilityName));

    count(sts.testIfPresent("Radio Operator should be present", m.radioOperator));
    count(sts.test("Form Latitude should be #EV", ref.formLocation.getLatitude(), m.formLocation.getLatitude()));
    count(sts.test("Form Longitude should be #EV", ref.formLocation.getLongitude(), m.formLocation.getLongitude()));

    var practiceSummary = new PracticeSummary(m, sts);
    practiceSummaries.add(practiceSummary);
  }
}
