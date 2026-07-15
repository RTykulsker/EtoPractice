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

package com.surftools.wimp.practice.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.BucketChooser;
import com.surftools.utils.FileUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.practice.generator.PracticeUtils;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.schedule.ScheduleManager;
import com.surftools.wimp.schedule.ScheduleRecord;
import com.surftools.wimp.utils.config.impl.PropertyFileConfigurationManager;

public class PracticeScheduleTool {
  private static final Logger logger = LoggerFactory.getLogger(PracticeScheduleTool.class);
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  @Option(name = "--config", usage = "practice configuration file name", required = true)
  private String configurationFileName;

  public static void main(String[] args) {
    var app = new PracticeScheduleTool();
    CmdLineParser parser = new CmdLineParser(app);
    try {
      parser.parseArgument(args);
      app.run();
    } catch (Exception e) {
      e.printStackTrace(System.err);
      parser.printUsage(System.err);
    }
  }

  private void run() throws Exception {
    logger.info("begin run");

    logger.info("");

    var cm = new PropertyFileConfigurationManager(configurationFileName, Key.values());
    logger.info("Using configuration file: " + configurationFileName);

    var rngSeedString = cm.getAsString(Key.GENERATOR_RNG_SEED, "2025");
    var rngSeed = Long.valueOf(rngSeedString);
    logger.info("rngSeed: " + rngSeed);
    var rng = new Random(rngSeed);

    var nYearsString = cm.getAsString(Key.GENERATOR_N_YEARS, "5");
    var nYears = Integer.valueOf(nYearsString);
    logger.info("nYears: " + nYears);
    final var startDate = LocalDate.of(2025, 1, 1);
    logger.info("startDate: " + startDate.toString());
    final var endDate = LocalDate.of(startDate.getYear() + nYears, 12, 31);
    logger.info("endDate: " + endDate.toString());

    var legacyEnabled = cm.getAsBoolean(Key.ENABLE_LEGACY, Boolean.FALSE);
    var inputList = defaultIRList(rng, legacyEnabled);
    var outputList = generateSchedule(startDate, endDate, inputList);
    var metaSchedulePathString = cm.getAsString(Key.PATH_META_SCHEDULE);
    var metaSchedulePath = Path.of(metaSchedulePathString);
    var metaScheduleFile = metaSchedulePath.toFile();

    if (metaScheduleFile.exists()) {
      var scheduleParentPath = metaSchedulePath.getParent();
      var historyPath = Path.of(scheduleParentPath.toString(), "schedule-history");
      FileUtils.makeDirIfNeeded(historyPath);
      var scheduleAttributes = Files.readAttributes(metaSchedulePath, BasicFileAttributes.class);
      FileTime creationTime = scheduleAttributes.lastModifiedTime();
      var instant = creationTime.toInstant();
      var dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
      var timeStamp = String.format("%02d%02d%02d-%02d%02d%02d", //
          dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(), //
          dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());
      var destinationPath = Path.of(historyPath.toString(), "meta-schedule-" + timeStamp + ".csv");
      Files.move(metaSchedulePath, destinationPath, StandardCopyOption.ATOMIC_MOVE);
    }

    WriteProcessor.writeTable(new ArrayList<IWritableTable>(outputList), metaSchedulePath);

    var today = LocalDate.now();
    var scheduleManager = new ScheduleManager(cm);
    var searchResult = scheduleManager.check(today);
    logger.info("searchResult: " + searchResult);
    logger.info("end run");
  }

  @Deprecated
  private List<InternalRecord> defaultIRList(Random rng, boolean legacyEnabled) {
    var ics213Chooser = new BucketChooser<MessageType>(List.of(MessageType.ICS_213), rng);
    var ics213IR = new InternalRecord("1st THU", 1, DayOfWeek.THURSDAY, null, null, ics213Chooser, true, null);

    var ics213RRChooser = new BucketChooser<MessageType>(List.of(MessageType.ICS_213_RR), rng);
    var ics213RRIR = new InternalRecord("2nd THU", 2, DayOfWeek.THURSDAY, null, null, ics213RRChooser, false, null);

    var hics259Chooser = new BucketChooser<MessageType>(List.of(MessageType.HICS_259), rng);
    var hics259IR = new InternalRecord("3rd THU", 3, DayOfWeek.THURSDAY, null, null, hics259Chooser, true, null);

    var ics205Chooser = new BucketChooser<MessageType>(List.of(MessageType.ICS_205), rng);
    var ics205IR = new InternalRecord("4th THU", 4, DayOfWeek.THURSDAY, null, null, ics205Chooser, true, null);

    var fsrChooser = new BucketChooser<MessageType>(List.of(MessageType.FIELD_SITUATION), rng);
    var fsrIR = new InternalRecord("5th THU", 5, DayOfWeek.THURSDAY, null, null, fsrChooser, true, null);

    List<InternalRecord> inputList = null;
    if (legacyEnabled) {
      inputList = List.of(ics213IR, ics213RRIR, hics259IR, ics205IR, fsrIR);
    } else {
      inputList = List.of(ics213IR, ics213RRIR, ics205IR, fsrIR);
    }

    return inputList;
  }

  protected List<ScheduleRecord> generateSchedule(LocalDate startDate, LocalDate endDate,
      List<InternalRecord> inputList) {

    var outputList = new ArrayList<ScheduleRecord>();
    var matches = new ArrayList<InternalRecord>();
    var date = startDate;
    while (!date.isAfter(endDate)) {
      var ordinal = PracticeUtils.getOrdinalDayOfWeek(date);
      var dayOfWeek = date.getDayOfWeek();
      var month = date.getMonth();
      var year = date.getYear();

      matches.clear();
      for (var input : inputList) {
        if (ordinal != input.ordinalDayOfWeek) {
          logger.debug("skipping IR: " + input.name + ", date: " + date.toString() + ", ordinal mismatch");
          continue;
        }

        if (!dayOfWeek.equals(input.dayOfWeek)) {
          logger.debug("skipping IR: " + input.name + ", date: " + date.toString() + ", day of week mismatch");
          continue;
        }

        if (input.month != null && !month.equals(input.month)) {
          logger.debug("skipping IR: " + input.name + ", date: " + date.toString() + ", month mismatch");
          continue;
        }

        if (input.year != null && year != input.year) {
          logger.debug("skipping IR: " + input.name + ", date: " + date.toString() + ", year mismatch");
          continue;
        }

        matches.add(input);
      } // end loop over IR

      if (matches.size() > 1) {
        throw new RuntimeException("Multiple IR matches for date: " + date.toString());
      }

      if (matches.size() == 1) {
        var input = matches.get(0);
        var chooser = input.chooser;
        var messageType = chooser.next();
        var output = new ScheduleRecord(input.name, date, messageType, input.canIncludeNextInstructions,
            input.extraData);
        outputList.add(output);
        logger.debug("adding IR: " + input.name + ", date: " + date.toString() + ", type: " + messageType.name());

      }

      date = date.plusDays(1);
    } // end loop over days

    return outputList;
  }

  record InternalRecord(String name, int ordinalDayOfWeek, DayOfWeek dayOfWeek, Month month, Integer year,
      BucketChooser<MessageType> chooser, boolean canIncludeNextInstructions, String extraData) {
  }
}
