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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.practice.misc.PracticeSummary;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class Ics213RRPracticeProcessor extends BasePracticeProcessor {
  private final Logger logger = LoggerFactory.getLogger(Ics213RRPracticeProcessor.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, MessageType.ICS_213_RR);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (Ics213RRMessage) message;
    var ref = (Ics213RRMessage) referenceMessage;

    count(sts.test_2line("Message Subject should be #EV", ref.subject, m.subject));
    count(sts.test("Message Location should be valid", m.msgLocation.isValid(), m.msgLocation.toString()));

    count(sts.test("Organization Name should be #EV", ref.organization, m.organization));
    count(sts.test("Incident Name should be #EV", ref.incidentName, m.incidentName));

    var formDateTime = LocalDateTime.parse(m.activityDateTime, DTF);
    count(sts.testOnOrAfter("Form Date and Time should be on or after #EV", windowOpenDT, formDateTime, DTF));
    count(sts.testOnOrBefore("Form Date and Time should be on or before #EV", windowCloseDT, formDateTime, DTF));

    count(sts.test("Resource Request Number should be #EV", ref.requestNumber, m.requestNumber));

    var maxLineItems = Math.min(ref.lineItems.size(), m.lineItems.size());
    if (m.lineItems.size() != ref.lineItems.size()) {
      logger
          .warn("### from: " + m.from + ",mId: " + m.messageId + ", m.items: " + m.lineItems.size() + ", ref.items: "
              + ref.lineItems.size());
    }
    for (var i = 0; i < maxLineItems; ++i) {
      var lineNumber = i + 1;
      sts.setExplanationPrefix("(line " + lineNumber + ")");
      var item = m.lineItems.get(i);
      var refItem = ref.lineItems.get(i);
      if (refItem.isEmpty()) {
        count(sts.testIfEmpty("Quantity should be empty", item.quantity()));
        count(sts.testIfEmpty("Kind should be empty", item.kind()));
        count(sts.testIfEmpty("Type should be empty", item.type()));
        count(sts.testIfEmpty("Item should be empty", item.item()));
        count(sts.testIfEmpty("Requested Date/Time should be empty", item.requestedDateTime()));
        count(sts.testIfEmpty("Estimated Date/Time should be empty", item.estimatedDateTime()));
        count(sts.testIfEmpty("Cost should be empty", item.cost()));
      } else {
        count(sts.test("Quantity should be #EV", refItem.quantity(), item.quantity()));

        if (refItem.kind().isEmpty()) {
          count(sts.testIfEmpty("Kind should be empty", item.kind()));
        } else {
          count(sts.test("Kind should be #EV", refItem.kind(), item.kind()));
        }

        if (refItem.type().isEmpty()) {
          count(sts.testIfEmpty("Type should be empty", item.type()));
        } else {
          count(sts.test("Type should be #EV", refItem.type(), item.type()));
        }

        count(sts.test_2line("Item should be #EV", refItem.item(), item.item()));

        count(sts.test("Requested Date/Time should be #EV", refItem.requestedDateTime(), item.requestedDateTime()));

        count(sts.testIfEmpty("Estimated Date/Time should be empty", item.estimatedDateTime()));
        count(sts.testIfEmpty("Cost should be empty", item.cost()));
      }
    }

    sts.setExplanationPrefix("");

    count(sts.test("Delivery/Reporting Location should be #EV", ref.delivery, m.delivery));
    count(sts.test("Substitutes should be #EV", ref.substitutes, m.substitutes));
    count(sts.test("Requested by should be #EV", ref.requestedBy, m.requestedBy));
    count(sts.test("Priority should be #EV", ref.priority, m.priority));
    count(sts.test("Approved by should be #EV", ref.approvedBy, m.approvedBy));

    count(sts.testIfEmpty("Logistics Order Number should be empty", m.logisticsOrderNumber));
    count(sts.testIfEmpty("Supplier Phone Number should be empty", m.supplierInfo));
    count(sts.testIfEmpty("Supplier Name should be empty", m.supplierName));
    count(sts.testIfEmpty("Supplier POC should be empty", m.supplierPointOfContact));
    count(sts.testIfEmpty("Supply Notes should be empty", m.supplyNotes));
    count(sts.testIfEmpty("Logistics Authorizer should be empty", m.logisticsAuthorizer));
    count(sts.testIfEmpty("Logistics Date/Time should be empty", m.logisticsDateTime));
    count(sts.testIfEmpty("Logistics Ordered by should be empty", m.orderedBy));
    count(sts.testIfEmpty("inance Comments should be empty", m.financeComments));
    count(sts.testIfEmpty("Finance Section Chief Name should be #EV", m.financeName));
    count(sts.testIfEmpty("Finance Date/Time should be empty", m.financeDateTime));

    var practiceSummary = new PracticeSummary(m, sts);
    practiceSummaries.add(practiceSummary);
  }
}
