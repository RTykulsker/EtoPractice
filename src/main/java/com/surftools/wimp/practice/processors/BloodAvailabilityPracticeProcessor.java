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
import com.surftools.wimp.message.BloodAvailabilityMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.practice.misc.PracticeSummary;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class BloodAvailabilityPracticeProcessor extends BasePracticeProcessor {

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, MessageType.BLOOD_AVAILABILITY);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (BloodAvailabilityMessage) message;
    var ref = (BloodAvailabilityMessage) referenceMessage;

    count(sts.testStartsWith("Message Subject should start with #EV", ref.subject, m.subject));
    count(sts.test("Message Location should be valid", m.msgLocation.isValid(), m.msgLocation.toString()));
    count(sts.test("THIS IS AN EXERCISE should be checked", m.isExercise));
    count(sts.testIfPresent("Form Date should be present", m.formDateTime));

    count(sts.test("Facility Name should be #EV", ref.facilityName, m.facilityName));
    count(sts.test("Facility Address should be #EV", ref.facilityAddress, m.facilityAddress));
    count(sts.test("Facility Contact Name should be #EV", ref.facilityContactName, m.facilityContactName));
    count(sts.test("Facility Phone Number should be #EV", ref.facilityPhoneNumber, m.facilityPhoneNumber));

    count(sts.test("RED BLOOD CELL O+ should be #EV", ref.redOPlus, m.redOPlus));

    count(sts.test("Comments should be #EV", ref.comments, m.comments));
    count(sts.test("Approved by should be #EV", ref.approvedBy, m.approvedBy));
    count(sts.test("Attach CSV should be No", ref.attachCSV, m.attachCSV));
    count(sts.test("Facility Location LATITUDE should be #EV", ref.formLatitude, ref.formLatitude));
    count(sts.test("Facility Location LONGITUDE should be #EV", ref.formLongitude, ref.formLongitude));

    practiceSummaries.add(new PracticeSummary(m, sts));
  }
}
