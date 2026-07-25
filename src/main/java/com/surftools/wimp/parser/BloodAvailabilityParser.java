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

package com.surftools.wimp.parser;

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.BloodAvailabilityMessage;
import com.surftools.wimp.message.ExportedMessage;

/**
 * parser for Bulletin
 *
 * @author bobt
 *
 */
public class BloodAvailabilityParser extends AbstractBaseParser {
  @Override
  public ExportedMessage parse(ExportedMessage message) {

    try {

      String xmlString = new String(message.attachments.get(MessageType.BLOOD_AVAILABILITY.rmsViewerName()));

      makeDocument(message.messageId, xmlString);

      var isExercise = getStringFromXml("isexercise").equals("** THIS IS AN EXERCISE **");

      var formDateTime = getStringFromXml("datetime");
      var facilityName = getStringFromXml("facilityname");
      var facilityAddress = getStringFromXml("address");
      var facilityContactName = getStringFromXml("contactname");
      var facilityPhoneNumber = getStringFromXml("phonenumber");
      var redOPlus = getStringFromXml("btype1");
      var redOMinus = getStringFromXml("btype2");
      var redAPlus = getStringFromXml("btype3");
      var redAMinus = getStringFromXml("btype4");
      var redBPlus = getStringFromXml("btype5");
      var redBMinus = getStringFromXml("btype6");
      var redABPlus = getStringFromXml("btype7");
      var redABMinus = getStringFromXml("btype8");
      var plasmaO = getStringFromXml("btype9");
      var plasmaA = getStringFromXml("btype10");
      var plasmaB = getStringFromXml("btype11");
      var plasmaAB = getStringFromXml("btype12");
      var comments = getStringFromXml("comments");
      var approvedBy = getStringFromXml("approved_name");
      var attachCSV = getStringFromXml("addattachment").equals("false") ? "No" : "Yes";
      var formLatitude = getStringFromXml("maplat");
      var formLongitude = getStringFromXml("maplon");
      var version = "";
      var templateVersion = getStringFromXml("templateversion");
      if (templateVersion != null) {
        var fields = templateVersion.split(" ");
        version = fields[fields.length - 1]; // last field
      }
      var expressVersion = getExpressVersion(message, "Senders Express Version:");

      var m = new BloodAvailabilityMessage(message, isExercise, formDateTime, //
          facilityName, facilityAddress, facilityContactName, facilityPhoneNumber, //
          redOPlus, redOMinus, redAPlus, redAMinus, redBPlus, redBMinus, redABPlus, redABMinus, //
          plasmaO, plasmaA, plasmaB, plasmaAB, //
          comments, approvedBy, attachCSV, formLatitude, formLongitude, version, expressVersion);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }
}
