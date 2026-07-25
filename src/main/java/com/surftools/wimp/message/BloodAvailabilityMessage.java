/**

The MIT License (MIT)

Copyright (c) 2022, Robert Tykulsker

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

package com.surftools.wimp.message;

import com.surftools.wimp.core.MessageType;

public class BloodAvailabilityMessage extends ExportedMessage {
  public final boolean isExercise;
  public final String formDateTime;
  public final String facilityName;
  public final String facilityAddress;
  public final String facilityContactName;
  public final String facilityPhoneNumber;
  public final String redOPlus;
  public final String redOMinus;
  public final String redAPlus;
  public final String redAMinus;
  public final String redBPlus;
  public final String redBMinus;
  public final String redABPlus;
  public final String redABMinus;
  public final String plasmaO;
  public final String plasmaA;
  public final String plasmaB;
  public final String plasmaAB;
  public final String comments;
  public final String approvedBy;
  public final String attachCSV;
  public final String formLatitude;
  public final String formLongitude;
  public final String version;
  public final String expressVersion;

  public BloodAvailabilityMessage(ExportedMessage exportedMessage, boolean isExercise, String formDateTime, //
      String facilityName, String facilityAddress, String facilityContactName, String facilityPhoneNumber, //
      String redOPlus, String redOMinus, String redAPlus, String redAMinus, String redBPlus, String redBMinus,
      String redABPlus, String redABMinus, //
      String plasmaO, String plasmaA, String plasmaB, String plasmaAB, //
      String comments, String approvedBy, String attachCSV, //
      String formLatitude, String formLongitude, String version, String expressVersion) {
    super(exportedMessage);
    this.isExercise = isExercise;
    this.formDateTime = formDateTime;
    this.facilityName = facilityName;
    this.facilityAddress = facilityAddress;
    this.facilityContactName = facilityContactName;
    this.facilityPhoneNumber = facilityPhoneNumber;
    this.redOPlus = redOPlus;
    this.redOMinus = redOMinus;
    this.redAPlus = redAPlus;
    this.redAMinus = redAMinus;
    this.redBPlus = redBPlus;
    this.redBMinus = redBMinus;
    this.redABPlus = redABPlus;
    this.redABMinus = redABMinus;
    this.plasmaO = plasmaO;
    this.plasmaA = plasmaA;
    this.plasmaB = plasmaB;
    this.plasmaAB = plasmaAB;
    this.comments = comments;
    this.approvedBy = approvedBy;
    this.attachCSV = attachCSV;
    this.formLatitude = formLatitude;
    this.formLongitude = formLongitude;
    this.version = version;
    this.expressVersion = expressVersion;
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  public static String[] getStaticHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
        "Latitude", "Longitude", "Is Exercise", "Form Date/Time", //
        "FacilityName", "FacilityAddress", "FacilityContact", "FacilityPhone", //
        "Red O+", "Red O-", "Red A+", "Red A-", "Red B+", "Red B-", "Red AB+", "Red AB-", //
        "Plasma O", "Plasma A", "Plasma B", "Plasma AB", //
        "Comments", "Approved By", "AttachCSV", "Form Latitude", "Form Longitude", "Version", "ExpressVersion",
        "File Name" };
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();

    // prefer form location to message location
    var location = mapLocation;
    var lat = location == null ? "" : location.getLatitude();
    var lon = location == null ? "" : location.getLongitude();

    return new String[] { messageId, from, to, subject, date, time, //
        lat, lon, String.valueOf(isExercise), formDateTime, //
        facilityName, facilityAddress, facilityContactName, facilityPhoneNumber, //
        redOPlus, redOMinus, redAPlus, redAMinus, redBPlus, redBMinus, redABPlus, redABMinus, //
        plasmaO, plasmaA, plasmaB, plasmaO, //
        comments, approvedBy, attachCSV, formLatitude, formLongitude, version, expressVersion, fileName };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.BLOOD_AVAILABILITY;
  }

  @Override
  public String getMultiMessageComment() {
    return comments;
  }
}
