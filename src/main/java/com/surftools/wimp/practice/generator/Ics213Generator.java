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

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.practice.generator.PracticeData.ExerciseIdMethod;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class Ics213Generator extends AbstractBasePracticeGenerator {

  @Override
  public void initialize(IConfigurationManager cm) {
    super.initialize(cm);
  }

  @Override
  public Ics213Message generateMessage(LocalDate date) {
    var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    var formSubject = "ETO Practice Exercise for " + dtf.format(date);
    var subject = "ICS-213: " + formSubject;
    var exportedMessage = makeExportedMessage(date, subject);

    var organization = "EmComm Training Organization";
    var incidentName = "Exercise Id: " + data.getExerciseId(ExerciseIdMethod.PHONE);
    var formFrom = data.nameChooser.next() + " / " + data.roleChooser.next();
    var formTo = data.nameChooser.next() + " / " + data.roleChooser.next();

    var formDate = NA;
    var formTime = NA;
    var formMessage = "ETO Weekly Practice Exercise for: " + dtf.format(date);
    var approvedBy = data.nameChooser.next();
    var position = data.roleChooser.next();
    var isExercise = true;
    var formLocation = LatLongPair.ZERO_ZERO;
    var version = NA;
    var expressVersion = NA;
    var dataSource = NA;

    var m = new Ics213Message(exportedMessage, organization, incidentName, //
        formFrom, formTo, formSubject, formDate, formTime, //
        formMessage, approvedBy, position, //
        isExercise, formLocation, version, expressVersion, dataSource);

    return m;
  }

  @Override
  public String generateIntructions(ExportedMessage message, LocalDate date) {
    var m = (Ics213Message) message;

    var sb = new StringBuilder();
    sb.append(generateInstructionHeader(date, "Complete an ICS-213 General Message"));
    sb.append(INDENT + "Setup: agency or group name: " + m.organization + NL);
    sb.append(INDENT + "THIS IS AN EXERCISE: (checked)" + NL);
    sb.append(INDENT + "Incident Name: " + m.incidentName + NL);
    sb.append(INDENT + "To (Name/Position): " + m.formTo + NL);
    sb.append(INDENT + "From (Name/Position): " + m.formFrom + NL);
    sb.append(INDENT + "Subject: " + m.formSubject + NL);
    sb.append(INDENT + "Date: (click in box and accept date)" + NL);
    sb.append(INDENT + "Time: (click in box and accept time)" + NL);
    sb.append(INDENT + "Message: " + m.formMessage + NL);
    sb.append(INDENT + "Approved by: " + m.approvedBy + NL);
    sb.append(INDENT + "Position / Title: " + m.position + NL);
    sb.append(generateInstructionTail());

    return sb.toString();
  }

}
