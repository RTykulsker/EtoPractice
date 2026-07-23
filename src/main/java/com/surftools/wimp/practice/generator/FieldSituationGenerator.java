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
import java.util.List;

import com.surftools.utils.BucketChooser;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class FieldSituationGenerator extends AbstractBasePracticeGenerator {

  @Override
  public void initialize(IConfigurationManager cm) {
    super.initialize(cm);
  }

  @Override
  public FieldSituationMessage generateMessage(LocalDate date) {
    var subject = "//WL2K R/ Routine/ Field Situation Report";
    var exportedMessage = makeExportedMessage(date, subject);

    var organization = "EmComm Training Organization";
    var precedence = "R/ Routine";
    var formDateTime = NA;
    var task = "ETO Weekly Practice";
    var formTo = "ETO-PRACTICE";
    var formFrom = NA;
    var isHelpNeeded = "NO";
    var neededHelp = "";

    var city = "Fort Collins";
    var county = "Larimer";
    var state = "CO";
    var territory = "";

    var formLocation = new LatLongPair("40.2503", "-103.7990");

    final var YES = "YES";
    final var NO = "NO";
    final var UNK = "Unknown - N/A";
    var statusChooser = new BucketChooser<String>(List.of(YES, NO, UNK), rng);

    var landlineStatus = statusChooser.next();
    var landlineComments = landlineStatus.equals(NO) ? "CenturyLink" : "";

    var voipStatus = UNK;
    var voipComments = "";

    var cellPhoneStatus = statusChooser.next();
    var cellPhoneComments = cellPhoneStatus.equals(NO) ? "Verizon" : "";

    var cellTextStatus = cellPhoneStatus;
    var cellTextComments = cellPhoneComments;

    var radioStatus = statusChooser.next();
    var radioComments = radioStatus.equals(NO) ? "KFRC FM" : "";

    var tvStatus = statusChooser.next();
    var tvComments = tvStatus.equals(NO) ? "KUSA" : "";

    var satTvStatus = UNK;
    var satTvComments = "";

    var cableTvStatus = statusChooser.next();
    var cableTvComments = cableTvStatus.equals(NO) ? "Xfinity" : "";

    var waterStatus = statusChooser.next();
    var waterComments = waterStatus.equals(NO) ? "Fort Collins Utilities" : "";

    var powerStatus = statusChooser.next();
    var powerComments = powerStatus.equals(NO) ? "Fort Collins Utilities" : "";

    var powerStable = statusChooser.next();
    var powerStableComments = powerStable.equals(NO) ? "Fort Collins Utilities" : "";

    var naturalGasStatus = statusChooser.next();
    var naturalGasComments = naturalGasStatus.equals(NO) ? "Xcel Energy" : "";

    var internetStatus = cableTvStatus;
    var internetComments = cableTvComments;

    var noaaStatus = YES;
    var noaaComments = "";

    var noaaAudioDegraded = NO;
    var noaaAudioDegradedComments = "";

    var additionalComments = "Exercise Id: " + data.getExerciseId();
    var poc = data.nameChooser.next();
    var formVersion = NA;
    var expressVersion = NA;

    var m = new FieldSituationMessage(//
        exportedMessage, organization, formLocation, //
        precedence, formDateTime, task, formTo, formFrom, isHelpNeeded, neededHelp, //
        city, county, state, territory, //
        landlineStatus, landlineComments, voipStatus, voipComments, //
        cellPhoneStatus, cellPhoneComments, cellTextStatus, cellTextComments, //
        radioStatus, radioComments, //
        tvStatus, tvComments, satTvStatus, satTvComments, cableTvStatus, cableTvComments, //
        waterStatus, waterComments, //
        powerStatus, powerComments, powerStable, powerStableComments, //
        naturalGasStatus, naturalGasComments, //
        internetStatus, internetComments, //
        noaaStatus, noaaComments, noaaAudioDegraded, noaaAudioDegradedComments, //
        additionalComments, poc, formVersion, expressVersion);

    return m;
  }

  @Override
  public String generateIntructions(ExportedMessage message, LocalDate date) {
    var m = (FieldSituationMessage) message;

    var sb = new StringBuilder();
    sb.append(generateInstructionHeader(date, "Complete a Field SituationReport Message"));

    sb.append(INDENT + "Setup: agency or group name: " + m.organization + NL);
    sb.append(INDENT + "Precedence: " + m.precedence + NL);
    sb.append(INDENT + "Date/Time: (click in box and accept date/time)" + NL);
    sb.append(INDENT + "Task #: " + m.task + NL);
    sb.append(INDENT + "From: <YOUR CALL>" + NL);
    sb.append(INDENT + "To: " + m.formTo + NL);
    sb.append(INDENT + "EMERGENT/LIFE SAFETY Need: " + m.isHelpNeeded + NL);

    sb.append(INDENT + "City: " + m.city + NL);
    sb.append(INDENT + "County: " + m.county + NL);
    sb.append(INDENT + "State: " + m.state + NL);
    sb.append(INDENT + "Latitude: " + m.formLocation.getLatitude() + NL);
    sb.append(INDENT + "Longitude: " + m.formLocation.getLongitude() + NL);

    sb.append(INDENT + "POTS landlines functioning: " + m.landlineStatus + NL);
    sb.append(INDENT + "POTS landlines provider if NO: " + m.landlineComments + NL);
    sb.append(INDENT + "VOIP landlines functioning: " + m.voipStatus + NL);
    sb.append(INDENT + "VOIP landlines provider if NO: " + m.voipComments + NL);
    sb.append(INDENT + "Cell phone voice calls functioning: " + m.cellPhoneStatus + NL);
    sb.append(INDENT + "Cell phone voice provider if NO: " + m.cellPhoneComments + NL);
    sb.append(INDENT + "Cell phone texts functioning: " + m.cellTextStatus + NL);
    sb.append(INDENT + "Cell phone texts provider if NO: " + m.cellTextComments + NL);

    sb.append(INDENT + "AM/FM Broadcast Stations functioning: " + m.radioStatus + NL);
    sb.append(INDENT + "Broadcast station callsign if NO: " + m.radioComments + NL);
    sb.append(INDENT + "OTA TV functioning: " + m.tvStatus + NL);
    sb.append(INDENT + "TV station if NO: " + m.tvComments + NL);
    sb.append(INDENT + "Satellite TV functioning: " + m.satTvStatus + NL);
    sb.append(INDENT + "Satellite TV provider if NO: " + m.satTvComments + NL);
    sb.append(INDENT + "Cable TV functioning: " + m.cableTvStatus + NL);
    sb.append(INDENT + "Cable TV provider if NO: " + m.cableTvComments + NL);

    sb.append(INDENT + "Public Water Works functioning: " + m.waterStatus + NL);
    sb.append(INDENT + "Public Water Works provider if NO: " + m.waterComments + NL);
    sb.append(INDENT + "Commercial Power functioning: " + m.powerStatus + NL);
    sb.append(INDENT + "Commercial Power provider if NO: " + m.powerComments + NL);
    sb.append(INDENT + "Commercial Power Stable: " + m.powerStableStatus + NL);
    sb.append(INDENT + "Commercial Power provider if NO: " + m.powerStableComments + NL);
    sb.append(INDENT + "Natural Gas supply functioning: " + m.naturalGasStatus + NL);
    sb.append(INDENT + "Natural Gas supply provider if NO: " + m.naturalGasComments + NL);
    sb.append(INDENT + "Internet functioning: " + m.internetStatus + NL);
    sb.append(INDENT + "Internet provider if NO: " + m.internetComments + NL);

    sb.append(INDENT + "NOAA weather radio functioning: " + m.noaaStatus + NL);
    sb.append(INDENT + "NOAA transmitter/frequency if NO: " + m.noaaComments + NL);
    sb.append(INDENT + "NOAA weather radio audio degraded: " + m.noaaAudioDegraded + NL);
    sb.append(INDENT + "NOAA transmitter/frequency if NO: " + m.noaaAudioDegradedComments + NL);

    sb.append(INDENT + "Additional Comments: " + m.additionalComments + NL);
    sb.append(INDENT + "POC: " + m.poc + NL);

    sb.append(generateInstructionTail());

    return sb.toString();
  }

}
