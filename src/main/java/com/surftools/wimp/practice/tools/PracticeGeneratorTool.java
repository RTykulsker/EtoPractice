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

package com.surftools.wimp.practice.tools;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.surftools.utils.FileUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.practice.generator.IGenerator;
import com.surftools.wimp.practice.generator.PracticeUtils;
import com.surftools.wimp.schedule.ScheduleManager;
import com.surftools.wimp.utils.config.IConfigurationManager;
import com.surftools.wimp.utils.config.impl.PropertyFileConfigurationManager;

/**
 * Program to generate many weeks work "data" for ETO weekly "practice"
 * semi-automatic exercises
 */
public class PracticeGeneratorTool {
  private static final Logger logger = LoggerFactory.getLogger(PracticeGeneratorTool.class);
  private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  @Option(name = "--config", usage = "practice configuration file name", required = true)
  private String configurationFileName;
  private IConfigurationManager cm;

  private String referenceDirName = null;
  private String exerciseYear = null;

  public static void main(String[] args) {
    var app = new PracticeGeneratorTool();
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
    cm = new PropertyFileConfigurationManager(configurationFileName, Key.values());
    logger.info("Using configuration file: " + configurationFileName);

    backupReferenceDir();
    var legacyDate = copyLegacyToReferency();

    var generatorMap = new HashMap<MessageType, IGenerator>();
    for (var type : MessageType.getAllSupportedTypes()) {
      var generatorName = "com.surftools.wimp.practice.generator." + type.makeParserName() + "Generator";
      try {
        var generatorClass = Class.forName(generatorName);
        var generator = (IGenerator) generatorClass.getDeclaredConstructor().newInstance();
        generator.initialize(cm);
        generatorMap.put(type, generator);
        logger.info("added generator: " + generatorName);
      } catch (Exception e) {
        logger.error("Couldn't create Generator for: " + type.toString() + ", " + e.getMessage());
        System.exit(1);
      }
    }

    var scheduleManager = new ScheduleManager(cm);
    var scheduleRecords = scheduleManager.getSchedules();
    for (var schedule : scheduleRecords) {
      if (!schedule.isPractice()) {
        continue;
      }

      var date = schedule.date();
      if (legacyDate != null && !date.isAfter(legacyDate)) {
        logger.info(
            "skipping: " + schedule.date() + ", " + schedule.name() + " before legacyDate: " + DTF.format(legacyDate));
        continue;
      }

      exerciseYear = String.valueOf(date.getYear());
      FileUtils.createDirectory(Path.of(referenceDirName, exerciseYear));

      var messageType = schedule.messageType();
      var generator = generatorMap.get(messageType);
      var m = generator.generateMessage(date, schedule);
      var instructions = generator.generateIntructions(m, date, schedule);

      var path = Path.of(referenceDirName, exerciseYear, date.toString());
      FileUtils.createDirectory(path);
      var objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
      try {
        var json = objectMapper.writeValueAsString(m);
        Files.writeString(Path.of(path.toString(), DTF.format(date) + "-reference.json"), json);
        Files.writeString(Path.of(path.toString(), DTF.format(date) + "-instructions.txt"), instructions);
      } catch (Exception e) {
        logger.error("Exception: " + e.getMessage());
      }
      var ord = PracticeUtils.getOrdinalDayOfWeek(date);
      var ordName = PracticeUtils.getOrdinalLabel(ord);
      logger.info("generated date: " + date + ", " + ordName + " " + date.getDayOfWeek().toString() + ", "
          + messageType.name());
    }
    logger.info("end run");
  }

  /**
   * copy reference json and instructions from "golden" legacy dir to newly
   * created reference dir if stricly before legacy date
   *
   * @return legacyDate
   */
  private LocalDate copyLegacyToReferency() throws Exception {
    referenceDirName = cm.getAsString(Key.PATH_REFERENCE);
    var referencePath = Path.of(referenceDirName);

    var legacyDateString = cm.getAsString(Key.GENERATOR_LEGACY_DATE);
    if (legacyDateString == null) {
      logger.info("LegacyDate null: skipping legacy processing");
      return null;
    }

    LocalDate legacyDate = null;
    if (legacyDateString != null) {
      try {
        legacyDate = LocalDate.parse(legacyDateString, DTF);
      } catch (Exception e) {
        throw new RuntimeException("Could not parse legacyDate: " + legacyDateString + ", " + e.getMessage());
      }
      logger.info("LegacyDate: " + DTF.format(legacyDate));
    }

    var legacyPathString = cm.getAsString(Key.PATH_REFERENCE_LEGACY);
    if (legacyPathString == null) {
      logger.info("Reference legacy path null: skipping legacy processing");
      return null;
    }
    var legacyPath = Path.of(legacyPathString);
    var legacyDir = legacyPath.toFile();
    if (!legacyDir.exists() || !legacyDir.isDirectory()) {
      throw new RuntimeException("Reference legacy path: " + legacyPathString + " not found or not a directory");
    }
    logger.info("LegacyPath: " + legacyPath.toString());

    // Iterate year folders: 2025, 2026, etc.
    try (DirectoryStream<Path> years = Files.newDirectoryStream(legacyPath)) {
      for (Path yearDir : years) {
        if (!Files.isDirectory(yearDir)) {
          continue;
        }

        String yearName = yearDir.getFileName().toString();
        if (!yearName.matches("\\d{4}")) {
          continue; // skip non-year folders
        }

        // Iterate date folders inside each year
        try (DirectoryStream<Path> dateDirs = Files.newDirectoryStream(yearDir)) {
          for (Path dateDir : dateDirs) {
            if (!Files.isDirectory(dateDir)) {
              continue;
            }

            String dateName = dateDir.getFileName().toString();
            LocalDate folderDate;

            try {
              folderDate = LocalDate.parse(dateName, DTF);
            } catch (Exception ex) {
              continue; // skip non-date folders
            }

            if (folderDate.isBefore(legacyDate)) {
              Path dest = referencePath.resolve(yearName).resolve(dateName);
              FileUtils.copyDirectory(dateDir, dest);
              Files.writeString(Path.of(dest.toString(), "legacy.txt"),
                  "legacy processing on: " + LocalDateTime.now().toString());
              logger.debug("Copying: " + dateDir.toString() + " to: " + dest.toString());
            } // end if copying
          } // end loop over dates in year
        } // end try over year
      } // end loop over years
    }

    return legacyDate;
  }

  private void backupReferenceDir() throws Exception {
    referenceDirName = cm.getAsString(Key.PATH_REFERENCE);
    var referencePath = Path.of(referenceDirName);
    var referenceDirAttributes = Files.readAttributes(referencePath, BasicFileAttributes.class);
    FileTime referenceDirCreationTime = referenceDirAttributes.creationTime();
    var instant = referenceDirCreationTime.toInstant();
    var rdate = LocalDate.ofInstant(instant, ZoneId.systemDefault());
    var time = LocalTime.ofInstant(instant, ZoneId.systemDefault());
    var timeStamp = String.format("%02d%02d%02d-%02d%02d%02d", //
        rdate.getYear(), rdate.getMonthValue(), rdate.getDayOfMonth(), //
        time.getHour(), time.getMinute(), time.getSecond());

    var refParentPath = referencePath.getParent();
    var historyPath = Path.of(refParentPath.toString(), "reference-history");
    FileUtils.makeDirIfNeeded(historyPath);
    var destinationPath = Path.of(historyPath.toString(), "reference-" + timeStamp);
    Files.move(referencePath, destinationPath);
    FileUtils.createDirectory(Path.of(referenceDirName));
  }

}
