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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.utils.config.IConfigurationManager;

public abstract class AbstractBasePracticeGenerator implements IGenerator {

  private boolean isInitialized = false;
  protected IConfigurationManager cm;
  protected PracticeData data;
  protected Random rng;

  protected static final String NA = "n/a";
  protected static final String NL = "\n";
  protected static final String INDENT = "    ";
  protected static final String INDENT2 = INDENT + INDENT;
  protected static final String INDENT3 = INDENT + INDENT2;
  public static String practiceInstructionURL = "https://emcomm-training.org/Winlink_Thursdays.html";

  public AbstractBasePracticeGenerator() {

  }

  @Override
  public void initialize(IConfigurationManager cm) {
    if (!isInitialized) {
      this.cm = cm;

      var rngSeed = cm.getAsString(Key.GENERATOR_RNG_SEED, "2025");
      rng = new Random(rngSeed.hashCode());
      data = new PracticeData(rng);

      isInitialized = true;
    }
  }

  protected String makeMessageId(String prefix, LocalDateTime dateTime) {
    final var dtf = DateTimeFormatter.ofPattern("MMddHHmmss");
    return prefix + dtf.format(dateTime);
  }

  protected ExportedMessage makeExportedMessage(LocalDate date, String subject) {
    LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.of(12, 0));
    var messageId = makeMessageId("PX", dateTime);
    var from = NA;
    var source = NA;
    var to = NA;
    var toList = NA;
    var ccList = NA;

    var msgLocation = LatLongPair.ZERO_ZERO;
    var locationSource = NA;
    var mime = NA;
    var plainContent = NA;
    Map<String, byte[]> attachments = new HashMap<String, byte[]>();
    boolean isP2p = false;
    String fileName = NA;
    List<String> lines = new ArrayList<String>();

    var exportedMessage = new ExportedMessage(messageId, from, source, to, toList, ccList, //
        subject, dateTime, //
        msgLocation, locationSource, //
        mime, plainContent, attachments, isP2p, fileName, lines);

    return exportedMessage;
  }

  /**
   * return the common header for instructions
   *
   * @param date
   * @param taskName
   * @return
   */
  protected String generateInstructionHeader(LocalDate date, String taskName) {
    var dtf = DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd");
    var dow_dtf = DateTimeFormatter.ofPattern("EEE yyyy-MM-dd");

    var sb = new StringBuilder(); // exercise instructions
    sb.append("ETO Exercise Instructions for " + dtf.format(date) + NL + NL);
    sb.append("Task: " + taskName + NL + NL);
    var windowOpenDate = date.minusDays(5);
    var windowCloseDate = date.plusDays(1);
    sb.append("Exercise window: " + dow_dtf.format(windowOpenDate) + " 00:00 UTC - " + //
        dow_dtf.format(windowCloseDate) + " 08:00 UTC" + NL + NL);
    sb.append("Use the following values when completing the form:" + NL);
    return sb.toString();
  }

  protected String generateInstructionTail() {
    var sb = new StringBuilder();
    sb.append(NL);
    sb.append("Ensure that you have a valid and appropriate Latitude and Longitude." + NL);
    sb.append(NL);
    sb.append("Send the message via the Session type of your choice to ETO-PRACTICE." + NL);
    sb.append(NL);
    sb.append("Refer to " + practiceInstructionURL + " for further instructions " + NL);
    sb.append(" about the weekly practice exercises and/or monthly training exercises." + NL);
    return sb.toString();
  }
}
