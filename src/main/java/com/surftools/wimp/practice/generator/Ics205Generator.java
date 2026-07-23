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

package com.surftools.wimp.practice.generator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.surftools.utils.BucketChooser;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics205Message;
import com.surftools.wimp.message.Ics205Message.RadioEntry;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class Ics205Generator extends AbstractBasePracticeGenerator {

  @Override
  public void initialize(IConfigurationManager cm) {
    super.initialize(cm);
  }

  @Override
  public Ics205Message generateMessage(LocalDate date) {
    final int nRadioEntries = 3;
    Ics205Message.setRadioEntriesToDisplay(nRadioEntries);

    var incidentName = "ETO Weekly Practice";
    var subject = "ICS 205 - " + incidentName;
    var exportedMessage = makeExportedMessage(date, subject);

    var organization = "EmComm Training Organization";
    var windowOpenDate = date.minusDays(5);
    var windowCloseDate = date.plusDays(1);
    var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    var dateFrom = dtf.format(windowOpenDate);
    var dateTo = dtf.format(windowCloseDate);
    var timeFrom = "00:00 UTC";
    var timeTo = "08:00 UTC";
    var radioItems = makeRadioEntries(nRadioEntries, date);
    for (var i = nRadioEntries; i < Ics205Message.MAX_RADIO_ENTRIES; ++i) {
      radioItems.add(RadioEntry.EMPTY);
    }
    var specialInstructions = "Exercise Id: " + data.getExerciseId();
    var approvedBy = data.nameChooser.next();
    var iapPage = String.valueOf(rng.nextInt(5, 10));

    var m = new Ics205Message(exportedMessage, organization, incidentName, NA, //
        dateFrom, dateTo, timeFrom, timeTo, //
        specialInstructions, approvedBy, NA, iapPage, //
        radioItems, NA, NA);
    return m;
  }

  @Override
  public String generateIntructions(ExportedMessage message, LocalDate date) {
    var m = (Ics205Message) message;

    var sb = new StringBuilder();
    sb.append(generateInstructionHeader(date, "Complete an ICS-205 Incident Radio Communications Plan Message"));

    sb.append("Use the following values when completing the form:" + NL);
    sb.append(INDENT + "Setup: agency or group name: " + m.organization + NL);
    sb.append(INDENT + "Incident name: " + m.incidentName + NL);
    sb.append(INDENT + "Date/Time: (click in box and accept date/time)" + NL);
    sb.append(INDENT + "Operational Period Date From: " + m.dateFrom + NL);
    sb.append(INDENT + "Operational Period Date To: " + m.dateTo + NL);
    sb.append(INDENT + "Operational Period Time From: " + m.timeFrom + NL);
    sb.append(INDENT + "Operational Period Time To: " + m.timeTo + NL);

    sb.append(INDENT + "Basic Radio Channel Use:" + NL);
    var lineNumber = 0;
    for (var item : m.radioEntries) {
      if (item.isEmpty()) {
        break;
      }

      ++lineNumber;
      sb.append(INDENT2 + "line " + lineNumber + NL); //
      sb.append(INDENT3 + "Ch #: " + item.channelNumber() + NL);
      sb.append(INDENT3 + "Function: " + item.function() + NL);
      sb.append(INDENT3 + "Channel Name: " + item.channelName() + NL);
      sb.append(INDENT3 + "Assignment: " + item.assignment() + NL);
      sb.append(INDENT3 + "RX Freq: " + item.rxFrequency() + NL);
      sb.append(INDENT3 + "RX N or W: " + item.rxNarrowWide() + NL);
      sb.append(INDENT3 + "RX Tone: " + item.rxTone() + NL);
      sb.append(INDENT3 + "Tx Freq: " + item.txFrequency() + NL);
      sb.append(INDENT3 + "TX N or W: " + item.txNarrowWide() + NL);
      sb.append(INDENT3 + "TX Tone: " + item.txTone() + NL);
      sb.append(INDENT3 + "Mode: " + item.mode() + NL);
      sb.append(INDENT3 + "Remarks: " + item.remarks() + NL);
    }

    sb.append(INDENT + "Special Instructions: " + m.specialInstructions + NL);
    sb.append(INDENT + "Approved by: " + m.approvedBy + NL);
    sb.append(INDENT + "Approved Date/Time: (click in box and accept date/time)" + NL);
    sb.append(INDENT + "IAP Page: " + m.iapPage + NL);
    sb.append(INDENT + "Attach CSV: (No)" + NL);
    sb.append(generateInstructionTail());

    return sb.toString();
  }

  public List<RadioEntry> makeRadioEntries(int desiredCount, LocalDate date) {
    List<RadioEntry> list = new ArrayList<>();

    if (desiredCount == 0) {
      return list;
    }

    final List<Boolean> booleans = List.of(Boolean.FALSE, Boolean.TRUE);
    final BucketChooser<Boolean> isRepeaterChooser = new BucketChooser<Boolean>(booleans, rng);
    final BucketChooser<Boolean> isBandChooser = new BucketChooser<Boolean>(booleans, rng);
    final BucketChooser<Boolean> isOffsetPositiveChooser = new BucketChooser<Boolean>(booleans, rng);
    final BucketChooser<Boolean> isSquelchToneChooser = new BucketChooser<Boolean>(booleans, rng);
    final BucketChooser<String> widthChooser = new BucketChooser<String>(List.of("W", "N"), rng);

    var entry = (RadioEntry) null;
    for (var count = 1; count <= 2; ++count) {
      var isBandVHF = isBandChooser.next();
      var width = widthChooser.next();
      var isRepeater = isRepeaterChooser.next();
      if (isRepeater) {
        var isOffsetPositive = isOffsetPositiveChooser.next();
        var isSquelchTone = isSquelchToneChooser.next();
        entry = makeRepeater(count, isBandVHF, isOffsetPositive, width, isSquelchTone);
      } else {
        entry = makeSimplex(count, isBandVHF, width);
      }
      list.add(entry);
      if (count == desiredCount) {
        return list;
      }
    } // end lines 1 and 2

    final var bonusChooser = new BucketChooser<String>(List.of("WX", "GMRS", "TAC"), rng);
    var bonus = bonusChooser.next();
    if (bonus.equals("WX")) {
      entry = makeWx(3);
    } else if (bonus.equals("GMRS")) {
      entry = makeGmrs(3);
    } else if (bonus.equals("TAC")) {
      entry = makeTac(3);
    }

    list.add(entry);
    if (desiredCount == 3) {
      return list;
    }

    // just make random repeaters and/or simplex channels
    for (var count = 4; count <= desiredCount; ++count) {
      var isBandVHF = isBandChooser.next();
      var isRepeater = isRepeaterChooser.next();
      var width = widthChooser.next();
      if (isRepeater) {
        var isOffsetPositive = isOffsetPositiveChooser.next();
        var isSquelchTone = isSquelchToneChooser.next();
        entry = makeRepeater(count, isBandVHF, isOffsetPositive, width, isSquelchTone);
      } else {
        entry = makeSimplex(count, isBandVHF, width);
      }
      list.add(entry);
    }

    return list;
  }

  private String makeRxFrequency(boolean isBandVhf, boolean isOffsetPositive) {
    double rxFreq = 0;
    if (isBandVhf) {
      rxFreq = 147_000;
      // Repeaters above 147 typically use positive offsets, while those below 147
      // often use negative offsets.
      if (isOffsetPositive) {
        rxFreq = rxFreq + (rng.nextInt(50) * 10);
      } else {
        rxFreq = rxFreq - (rng.nextInt(100) * 10);
      }
    } else {
      rxFreq = 442_000;
      // Repeaters above 442 generally use +5 offset, while those below 445 may use –5
      // offset
      if (isOffsetPositive) {
        rxFreq = rxFreq + (rng.nextInt(60) * 50);
      } else {
        rxFreq = rxFreq - (rng.nextInt(60) * 50);
      }
    }
    // convert from Hz to double, to 3 digits as String;
    var d = rxFreq / 1000d;
    var rxFreqString = String.format("%.3f", d);
    return rxFreqString;
  }

  private String makeTxFrequency(String rxFreqString, boolean isBandVhf, boolean isOffsetPositive) {
    var rxFreqDouble = Double.parseDouble(rxFreqString);
    var offsetDouble = isBandVhf ? 0.6d : 5.0d;
    var multDouble = isOffsetPositive ? +1 : -1;
    var txFreqDouble = rxFreqDouble + (multDouble * offsetDouble);
    var txFreqString = String.format("%.3f", txFreqDouble);
    return txFreqString;
  }

  private RadioEntry makeRepeater(int rowNumber, boolean isBandVHF, boolean isOfsetPostive, String widthName,
      boolean isSquelchTone) {
    var rxFrequency = makeRxFrequency(isBandVHF, isOfsetPostive);
    var txFrequency = makeTxFrequency(rxFrequency, isBandVHF, isBandVHF);
    var txTone = getTone();
    var rxTone = getTone(txTone, isSquelchTone);
    var function = "Coordination";
    var channelName = "Repeater";
    var assignment = "amateur";
    var remarks = "";
    if (rowNumber <= 2) {
      remarks = "Primary repeater";
    } else if (rowNumber == 4) {
      remarks = "Secondary repeater";
    } else {
      remarks = "Backup repeater";
    }
    var entry = new RadioEntry(rowNumber, "", String.valueOf(rowNumber), //
        function, channelName, assignment, //
        rxFrequency, widthName, rxTone, //
        txFrequency, widthName, txTone, //
        "A", remarks);

    return entry;
  }

  private RadioEntry makeSimplex(int rowNumber, boolean isBandVhf, String widthName) {

    final var twoMeterSimplex = List.of("146.400", "146.415", "146.430", "146.445", "146.460", "146.475", "146.490",
        "146.505", "146.535", "146.550", "146.565", "146.580", "146.595", "147.405", "147.420", "147.435", "147.450",
        "147.465", "147.480", "147.495", "147.510", "147.525", "147.540", "147.555", "147.570", "147.585");
    final var twoMeterChooser = new BucketChooser<String>(twoMeterSimplex, rng);
    final var seventySimplex = List.of("445.925", "445.950", "445.975", "446.025", "446.050", "446.075", "446.100",
        "446.125", "446.150", "446.175");
    final var seventyChooser = new BucketChooser<String>(seventySimplex, rng);

    var chooser = isBandVhf ? twoMeterChooser : seventyChooser;
    var rxFreq = chooser.next();

    var function = "Tactical";
    var channelName = "Simplex";
    var assignment = "amateur";
    var remarks = "";
    if (rowNumber <= 2) {
      remarks = "Primary simplex";
    } else {
      remarks = "Secondary simplex";
    }

    var entry = new RadioEntry(rowNumber, "", String.valueOf(rowNumber), //
        function, channelName, assignment, //
        rxFreq, widthName, "", //
        rxFreq, widthName, "", //
        "A", remarks);
    return entry;
  }

  private RadioEntry makeWx(int rowNumber) {
    final var list = List.of( //
        "WX1 - 162.400", "WX2 - 162.425", "WX3 - 162.450", "WX4 - 162.475", "WX5 - 162.500", "WX6 - 162.525",
        "WX7 - 162.550");
    final var chooser = new BucketChooser<String>(list, rng);
    var data = chooser.next();
    var fields = data.split(" - ");
    var name = fields[0];
    var rxFreq = fields[1];

    var entry = new RadioEntry(rowNumber, "", String.valueOf(rowNumber), //
        "Information", name, "weather", //
        rxFreq, "W", "", //
        "", "", "", //
        "A", "Receive only. Do not Transmit!");
    return entry;
  }

  private RadioEntry makeGmrs(int rowNumber) {
    final var list = List.of(//
        "GMRS 1 - 462.5625", "GMRS 2 - 462.5875", "GMRS 3 - 462.6125", "GMRS 4 - 462.6375", "GMRS 5 - 462.6625",
        "GMRS 6 - 462.6875", "GMRS 7 - 462.7125", "GMRS 8 - 467.5625", "GMRS 9 - 467.5875", "GMRS 10 - 467.6125",
        "GMRS 11 - 467.6375", "GMRS 12 - 467.6625", "GMRS 13 - 467.6875", "GMRS 14 - 467.7125", "GMRS 15 - 462.5500",
        "GMRS 16 - 462.5750", "GMRS 17 - 462.6000", "GMRS 18 - 462.6250", "GMRS 19 - 462.6500", "GMRS 20 - 462.6750",
        "GMRS 21 - 462.7000", "GMRS 22 - 462.7250");
    final var chooser = new BucketChooser<String>(list, rng);
    var data = chooser.next();
    var fields = data.split(" - ");
    var name = fields[0];
    var rxFreq = fields[1];

    var entry = new RadioEntry(rowNumber, "", String.valueOf(rowNumber), //
        "Information", name, "GMRS", //
        rxFreq, "N", "", //
        rxFreq, "N", "", //
        "A", "Do not Transmit without GMRS license!");
    return entry;
  }

  private RadioEntry makeTac(int rowNumber) {

    final var list = List.of("VCALL10 - 155.7525", "VTAC11 - 151.1375", "VTAC12 - 154.4525", "VTAC13 - 158.7375",
        "VTAC14 - 159.4725", "VTAC17 - 161.8500", "UCALL40 - 453.2125", "UTAC41 - 453.4625", "UTAC42 - 453.7125",
        "UTAC43 - 453.8625");
    final var chooser = new BucketChooser<String>(list, rng);
    var data = chooser.next();
    var fields = data.split(" - ");

    var name = fields[0];
    var rxFreq = fields[1];

    // CTCSS tone 156.7 Hz is commonly used for analog FM operation
    var entry = new RadioEntry(rowNumber, "", String.valueOf(rowNumber), //
        "InterOp", name, "public safety", //
        rxFreq, "N", "156.7", //
        "", "", "", //
        "A", "Receive only. Do not Transmit!");
    return entry;
  }

  private String getTone() {
    final var list = List.of( //
        "67.0", "69.3", "71.9", "74.4", "77.0", "79.7", "82.5", "85.4", "88.5", "91.5", "94.8", "97.4", "100.0",
        "103.5", "107.2", "110.9", "114.8", "118.8", "123.0", "127.3", "131.8", "136.5", "141.3", "146.2", "151.4",
        "156.7", "159.8", "162.2", "165.5", "167.9", "171.3", "173.8", "177.3", "179.9", "183.5", "186.2", "189.9",
        "192.8", "196.6", "199.5", "203.5", "206.5", "210.7", "218.1", "225.7", "229.1", "233.6", "241.8", "250.3",
        "254.1");
    final var chooser = new BucketChooser<String>(list, rng);
    var freq = chooser.next();

    return freq;
  }

  private String getTone(String txTone, boolean isSquelchTone) {
    return isSquelchTone ? txTone : "";
  }
}
