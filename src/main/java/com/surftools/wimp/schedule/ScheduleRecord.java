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

package com.surftools.wimp.schedule;

import java.time.LocalDate;

import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.practice.generator.PracticeUtils;

public record ScheduleRecord(String name, LocalDate date, MessageType messageType,
    boolean canIncludeNextInstructions, String extraData) implements IWritableTable {

  @Override
  public int compareTo(IWritableTable other) {
    var o = (ScheduleRecord) other;
    return this.date.compareTo(o.date);
  }

  @Override
  public String[] getHeaders() {
    final var headers = new String[] { "Date", "Name", "MessageType", "Can Include Instructions", "Extra Data",
        "Ordinal", "Day Of Week" };
    return headers;
  }

  @Override
  public String[] getValues() {
    return new String[] { date.toString(), name, messageType.name(), String.valueOf(canIncludeNextInstructions),
        extraData == null ? "" : extraData, String.valueOf(PracticeUtils.getOrdinalDayOfWeek(date)),
        date.getDayOfWeek().toString() };
  }

  public static ScheduleRecord from(String[] fields) {
    var messageType = MessageType.fromString(fields[2].toLowerCase());
    if (messageType == null) {
      throw new RuntimeException("Could not get messageType for: " + fields[2] + " on date: " + fields[0]);
    }
    return new ScheduleRecord(fields[1], LocalDate.parse(fields[0]), messageType, Boolean.parseBoolean(fields[3]),
        fields[4]);
  }
}