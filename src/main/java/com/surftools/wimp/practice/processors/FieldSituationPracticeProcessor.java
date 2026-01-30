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
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.practice.misc.PracticeSummary;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class FieldSituationPracticeProcessor extends BasePracticeProcessor {

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, MessageType.FIELD_SITUATION);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (FieldSituationMessage) message;
    var ref = (FieldSituationMessage) referenceMessage;

    count(sts.testStartsWith("Message Subject should start with #EV", ref.subject, m.subject));
    count(sts.test("Message Location should be valid", m.msgLocation.isValid(), m.msgLocation.toString()));
    count(sts.test("Form Location should be valid", m.formLocation.isValid(), m.formLocation.toString()));

    count(sts.test("Organization Name should be #EV", ref.organization, m.organization));
    count(sts.test("Precedence should be #EV", ref.precedence, m.precedence));

    count(sts.testIfPresent("Form Date/Time should be present", m.formDateTime));
    count(sts.test("Task # should be #EV", ref.task, m.task));
    count(sts.test("Emergent/Life Safety need should be #EV", ref.isHelpNeeded, m.isHelpNeeded));
    count(sts.test("City should be #EV", ref.city, m.city));
    count(sts.test("County should be #EV", ref.county, m.county));
    count(sts.testIfEmpty("Territory should be empty", m.territory));
    count(sts.testAsDouble("LAT should be #EV", ref.formLocation.getLatitude(), m.formLocation.getLatitude()));
    count(sts.testAsDouble("LON should be #EV", ref.formLocation.getLongitude(), m.formLocation.getLongitude()));

    count(sts.test("POTS landlines functioning should be #EV", ref.landlineStatus, m.landlineStatus));
    if (ref.landlineStatus.equals("NO")) {
      count(sts.test("POTS provider should be #EV", ref.landlineComments, m.landlineComments));
    } else {
      count(sts.testIfEmpty("POTS provider should be empty", m.landlineComments));
    }

    count(sts.test("VOIP landlines functioning should be #EV", ref.voipStatus, m.voipStatus));
    if (ref.voipStatus.equals("NO")) {
      count(sts.test("VOIP provider should be #EV", ref.voipComments, m.voipComments));
    } else {
      count(sts.testIfEmpty("VOIP provider should be empty", m.voipComments));
    }

    count(sts.test("Cell phone voice functioning should be #EV", ref.cellPhoneStatus, m.cellPhoneStatus));
    if (ref.cellPhoneStatus.equals("NO")) {
      count(sts.test("Cell phone voice provider should be #EV", ref.cellPhoneComments, m.cellPhoneComments));
    } else {
      count(sts.testIfEmpty("Cell phone voice provider should be empty", m.cellPhoneComments));
    }

    count(sts.test("Cell phone text functioning should be #EV", ref.cellTextStatus, m.cellTextStatus));
    if (ref.cellTextStatus.equals("NO")) {
      count(sts.test("Cell phone text provider should be #EV", ref.cellTextComments, m.cellTextComments));
    } else {
      count(sts.testIfEmpty("Cell text voice provider should be empty", m.cellTextComments));
    }

    count(sts.test("AM/FM Broadcast functioning should be #EV", ref.radioStatus, m.radioStatus));
    if (ref.radioStatus.equals("NO")) {
      count(sts.test("AM/FM stations should be #EV", ref.radioComments, m.radioComments));
    } else {
      count(sts.testIfEmpty("AM/FM stations should be empty", m.radioComments));
    }

    count(sts.test("OTA TV functioning should be #EV", ref.tvStatus, m.tvStatus));
    if (ref.tvStatus.equals("NO")) {
      count(sts.test("OTA TV stations should be #EV", ref.tvComments, m.tvComments));
    } else {
      count(sts.testIfEmpty("OTA TV stations should be empty", m.tvComments));
    }

    count(sts.test("Satellite TV functioning should be #EV", ref.satTvStatus, m.satTvStatus));
    if (ref.satTvStatus.equals("NO")) {
      count(sts.test("Satellite TV provider should be #EV", ref.satTvComments, m.satTvComments));
    } else {
      count(sts.testIfEmpty("Satellite TV provider should be empty", m.satTvComments));
    }

    count(sts.test("Cable TV functioning should be #EV", ref.cableTvStatus, m.cableTvStatus));
    if (ref.cableTvStatus.equals("NO")) {
      count(sts.test("Cable TV provider should be #EV", ref.cableTvComments, m.cableTvComments));
    } else {
      count(sts.testIfEmpty("Cable TV provider should be empty", m.cableTvComments));
    }

    count(sts.test("Public Water Works functioning should be #EV", ref.waterStatus, m.waterStatus));
    if (ref.waterStatus.equals("NO")) {
      count(sts.test("Public Water Works provider should be #EV", ref.waterComments, m.waterComments));
    } else {
      count(sts.testIfEmpty("Public Water Works provider should be empty", m.waterComments));
    }

    count(sts.test("Commercial Power functioning should be #EV", ref.powerStatus, m.powerStatus));
    if (ref.powerStatus.equals("NO")) {
      count(sts.test("Commercial Power provider should be #EV", ref.powerComments, m.powerComments));
    } else {
      count(sts.testIfEmpty("Commercial Power provider should be empty", m.powerComments));
    }

    count(sts.test("Commercial Power stable should be #EV", ref.powerStableStatus, m.powerStableStatus));
    if (ref.powerStableStatus.equals("NO")) {
      count(sts.test("Commercial Power Stable provider should be #EV", ref.powerStableComments, m.powerStableComments));
    } else {
      count(sts.testIfEmpty("Commercial Power Stable provider should be empty", m.powerStableComments));
    }

    count(sts.test("Natural Gas supply functioning be #EV", ref.naturalGasStatus, m.naturalGasStatus));
    if (ref.naturalGasStatus.equals("NO")) {
      count(sts.test("Natural Gas provider should be #EV", ref.naturalGasComments, m.naturalGasComments));
    } else {
      count(sts.testIfEmpty("Natural Gas provider should be empty", m.naturalGasComments));
    }

    count(sts.test("Internet functioning be #EV", ref.internetStatus, m.internetStatus));
    if (ref.internetStatus.equals("NO")) {
      count(sts.test("Internet provider should be #EV", ref.internetComments, m.internetComments));
    } else {
      count(sts.testIfEmpty("Internet provider should be empty", m.internetComments));
    }

    count(sts.test("NOAA Weather Radio functioning be #EV", ref.noaaStatus, m.noaaStatus));
    if (ref.noaaStatus.equals("NO")) {
      count(sts.test("NOAA Weather Radio station should be #EV", ref.noaaComments, m.noaaComments));
    } else {
      count(sts.testIfEmpty("NOAA Weather Radio station provider should be empty", m.noaaComments));
    }

    count(sts.test("NOAA Weather audio degraded be #EV", ref.noaaAudioDegraded, m.noaaAudioDegraded));
    if (ref.noaaAudioDegraded.equals("YES")) {
      count(sts.test("NOAA Weather Radio degraded station should be #EV", ref.noaaAudioDegradedComments,
          m.noaaAudioDegradedComments));
    } else {
      count(
          sts.testIfEmpty("NOAA Weather Radio degraded station provider should be empty", m.noaaAudioDegradedComments));
    }

    count(sts.test("Additional comments should be #EV", ref.additionalComments, m.additionalComments));
    count(sts.test("POC should be #EV", ref.poc, m.poc));

    var practiceSummary = new PracticeSummary(m, sts);
    practiceSummaries.add(practiceSummary);
  }
}
