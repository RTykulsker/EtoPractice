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

package com.surftools.wimp.practice.misc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;

public class PracticeSummary implements IWritableTable {
  public String from;
  public String to;
  public LatLongPair location;
  public LocalDateTime dateTime;
  public List<String> explanations;
  public String messageId;
  public String messageType;
  public ExportedMessage m;

  public static final String perfectMessageText = "Perfect messages!";
  public static final int perfectMessageCount = 0; // in case we need to adjust

  public PracticeSummary(ExportedMessage m, SimpleTestService sts) {
    this.from = m.from;
    this.to = m.to;
    this.location = (m.getMessageType() == MessageType.FIELD_SITUATION) ? m.msgLocation : m.mapLocation;
    this.dateTime = m.sortDateTime;
    this.messageId = m.messageId;
    this.explanations = sts.getExplanations();
    this.m = m;
  }

  @Override
  public int compareTo(IWritableTable o) {
    var other = (PracticeSummary) o;
    return from.compareTo(other.from);
  }

  @Override
  public String[] getHeaders() {
    var list = new ArrayList<String>(
        List.of("From", "To", "Latitude", "Longitude", "Date", "Time", "Feedback Count", "Feedback", "Message Id"));
    return list.toArray(new String[list.size()]);
  }

  public int getFeedbackCount() {
    return explanations.size() - perfectMessageCount;
  }

  public String getFeedback() {
    if (explanations.size() > 0) {
      return String.join("\n", explanations);
    } else {
      return perfectMessageText;
    }
  }

  @Override
  public String[] getValues() {
    var latitude = location == null ? "0.0" : location.getLatitude();
    var longitude = location == null ? "0.0" : location.getLongitude();
    var date = dateTime == null ? "" : dateTime.toLocalDate().toString();
    var time = dateTime == null ? "" : dateTime.toLocalTime().toString();

    var nsTo = to == null ? "(null)" : to;

    var list = new ArrayList<String>(List.of(from, nsTo, latitude, longitude, date, time,
        String.valueOf(getFeedbackCount()), getFeedback(), messageId));
    return list.toArray(new String[list.size()]);
  }
}
