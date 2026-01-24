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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.MessageManager;
import com.surftools.wimp.practice.generator.PracticeUtils;
import com.surftools.wimp.practice.misc.PracticeJsonMessageDeserializer;
import com.surftools.wimp.processors.std.PipelineProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;
import com.surftools.wimp.utils.config.impl.PropertyFileConfigurationManager;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;

public class PracticeProcessorTool {
  public static final String REFERENCE_MESSAGE_KEY = "referenceMessage";
  public static final String INSTRUCTIONS_KEY = "instructions";
  public static final String CONFIGURATION_FILE_KEY = "configurationFileName";

  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private static final Logger logger = LoggerFactory.getLogger(PracticeProcessorTool.class);

  @Option(name = "--exerciseDate", usage = "date of practice exercise in yyyy-MM-dd format", required = true)
  private String exerciseDateString = null;

  @Option(name = "--enableFinalize", usage = "to rename output,  email to ETO folks upon completion", required = false)
  private boolean enableFinalize = false;

  @Option(name = "--config", usage = "practice onfiguration file name", required = true)
  private String configurationFileName;

  public static void main(String[] args) {
    var tool = new PracticeProcessorTool();
    CmdLineParser parser = new CmdLineParser(tool);
    try {
      parser.parseArgument(args);
      tool.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  public void run() {
    try {
      var cm = new PropertyFileConfigurationManager(configurationFileName, Key.values());
      exerciseDateString = parse(exerciseDateString, cm);
      addDatedLogger(cm);

      logger.info("begin run");
      var exerciseDate = LocalDate.parse(exerciseDateString);
      if (exerciseDate.getDayOfWeek() != DayOfWeek.THURSDAY) {
        throw new RuntimeException("Exercise Date: " + exerciseDateString + " must be a THURSDAY");
      }

      var ord = PracticeUtils.getOrdinalDayOfWeek(exerciseDate);
      var ordinalList = new ArrayList<Integer>(PracticeGeneratorTool.VALID_ORDINALS);
      Collections.sort(ordinalList);
      var ordinalLabels = ordinalList.stream().map(i -> PracticeUtils.getOrdinalLabel(i)).toList();
      if (!PracticeGeneratorTool.VALID_ORDINALS.contains(ord)) {
        throw new RuntimeException("Exercise Date: " + exerciseDate.toString() + " is NOT one of "
            + String.join(",", ordinalLabels) + " THURSDAYS");
      }

      var messageType = PracticeGeneratorTool.MESSAGE_TYPE_MAP.get(ord);
      logger.info("Exercise Date: " + exerciseDate.toString() + ", " + PracticeUtils.getOrdinalLabel(ord)
          + " Thursday; exercise message type: " + messageType.toString());

      var exercisesPathName = cm.getAsString(Key.PATH_EXERCISES);
      logger.info("exercises home" + exercisesPathName);

      // fail fast on reading reference
      var referencePathName = cm.getAsString(Key.PATH_REFERENCE);
      logger.info("reference home: " + referencePathName);
      var exerciseYearString = String.valueOf(exerciseDate.getYear());
      var referencePath = Path.of(referencePathName, exerciseYearString, exerciseDateString,
          exerciseDateString + "-reference.json");
      var jsonString = Files.readString(referencePath);
      var deserializer = new PracticeJsonMessageDeserializer();
      var referenceMessage = deserializer.deserialize(jsonString, messageType);

      // make exercises folder if needed
      FileUtils.createDirectory(Path.of(exercisesPathName, exerciseYearString, exerciseDateString));
      FileUtils.createDirectory(Path.of(exercisesPathName, exerciseYearString, exerciseDateString, "input"));

      var winlinkCallsign = cm.getAsString(Key.WINLINK_CALLSIGN);
      logger.info("Winlink callsign: " + winlinkCallsign);

      var enableLegacy = cm.getAsBoolean(Key.ENABLE_LEGACY, Boolean.FALSE);
      logger.info("enable Legacy (3rd week practice instructions send on 2nd week): " + enableLegacy);

      final var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      var nextExerciseDate = exerciseDate.plusDays((ord == 2 && !enableLegacy) ? 14 : 7);
      var nextExerciseYear = nextExerciseDate.getYear();
      var nextExerciseDateString = dtf.format(nextExerciseDate);
      var instructionPath = Path.of(referencePathName, String.valueOf(nextExerciseYear), nextExerciseDateString,
          nextExerciseDateString + "-instructions.txt");
      var instructionText = Files.readString(instructionPath);
      var sb = new StringBuilder();
      sb.append("\n\n");
      if (ord == 2 && !enableLegacy) {
        sb.append("INSTRUCTIONS for next week:" + "\n");
        sb.append("Next Thursday is a \"Third Thursday Training Exercise\"," + "\n");
        sb.append(
            "so look for instructions on our web site at https://emcomm-training.org/Winlink_Thursdays.html" + "\n");
        sb.append("However, here are the " + instructionText + "\n");
      } else {
        sb.append("INSTRUCTIONS for " + instructionText + "\n");
      }
      instructionText = sb.toString();

      // create the rest of our configuration on the fly
      cm.putString(Key.EXERCISE_DATE, exerciseDateString);
      cm.putString(Key.EXERCISE_NAME, "ETO Weekly Practice for " + exerciseDateString);

      cm.putString(Key.EXPECTED_MESSAGE_TYPES, messageType.toString());

      var windowOpenDate = exerciseDate.minusDays(5);
      cm.putString(Key.EXERCISE_WINDOW_OPEN, dtf.format(windowOpenDate) + " 00:00");
      var windowCloseDate = exerciseDate.plusDays(1);
      cm.putString(Key.EXERCISE_WINDOW_CLOSE, dtf.format(windowCloseDate) + " 08:00");

      cm.putString(Key.PIPELINE_STDIN, "Read,Classifier,Acknowledgement,Deduplication");
      cm.putString(Key.PIPELINE_MAIN, "Ics213,Ics213RR,Ics205,Hics259,FieldSituation");
      // cm.putString(Key.PIPELINE_STDOUT,
      // "Write,MissedExercise,HistoryMap,Cleanup,Upload,EmailNotification");
      cm.putString(Key.PIPELINE_STDOUT, "Write,HistoryMap,Cleanup,Publication,EmailNotification");

      var edPrefix = "com.surftools.wimp.practice.misc.Practice";
      cm.putString(Key.ALL_FEEDBACK_TEXT_EDITOR, edPrefix + "AllFeedbackTextEditor");
      cm.putString(Key.BODY_TEXT_EDITOR, edPrefix + "BodyTextEditor");

      cm.putString(Key.OUTBOUND_MESSAGE_SOURCE, winlinkCallsign);
      cm.putString(Key.OUTBOUND_MESSAGE_SENDER, "ETO-PRACTICE");
      cm.putString(Key.OUTBOUND_MESSAGE_SUBJECT, "ETO Practice Exercise Feedback");

      cm.putBoolean(Key.ENABLE_FINALIZE, enableFinalize);

      var mm = new MessageManager();
      mm.putContextObject(REFERENCE_MESSAGE_KEY, referenceMessage);
      mm.putContextObject(INSTRUCTIONS_KEY, instructionText);
      mm.putContextObject(CONFIGURATION_FILE_KEY, configurationFileName);

      var pipeline = new PipelineProcessor();
      pipeline.initialize(cm, mm);
      pipeline.process();
      pipeline.postProcess();

    } catch (Exception e) {
      logger.error("Exception: " + e.getLocalizedMessage());
      e.printStackTrace();
    }
    logger.info("end run");
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void addDatedLogger(IConfigurationManager cm) {
    // Get the LoggerContext
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    // Create a PatternLayoutEncoder
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(loggerContext);
    encoder.setPattern("%d{HH:mm:ss.SSS} %-5level %class{0}.%M - %msg%n");
    encoder.start();

    // Create a FileAppender
    FileAppender fileAppender = new FileAppender();
    fileAppender.setContext(loggerContext);
    fileAppender.setAppend(false);

    var date = LocalDate.parse(exerciseDateString);
    var exerciseYear = date.getYear();
    var exerciseYearString = String.valueOf(exerciseYear);

    var exercisesPathName = cm.getAsString(Key.PATH_EXERCISES);
    var exercisePath = Path.of(exercisesPathName, exerciseYearString, exerciseDateString);
    var exercisePathName = exercisePath.toString();
    var outputPath = Path.of(exercisePathName, "output");
    FileUtils.deleteDirectory(outputPath);
    FileUtils.makeDirIfNeeded(outputPath.toString());
    var outputPathName = outputPath.toString();
    var logPath = Path.of(outputPathName, exerciseDateString + "-log.txt");
    fileAppender.setFile(logPath.toString());

    fileAppender.setEncoder(encoder);
    fileAppender.start();

    // Cast to Logback's Logger to access addAppender()
    var rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(fileAppender);
  }

  /**
   * return a date string if passed a known symbolic value
   *
   * @param input
   * @param cm
   * @return
   */
  private String parse(String input, IConfigurationManager cm) {
    if (input == null) {
      return input;
    }

    final var knownSet = Set.of("last", "current", "today", "this", "next");
    var s = input.toLowerCase();
    if (!knownSet.contains(s)) {
      return s;
    }

    var legacyEnabled = cm.getAsBoolean(Key.ENABLE_LEGACY, Boolean.FALSE);

    var date = LocalDate.now();
    switch (s) {
    case "last":
      while (true) {
        date = date.minusDays(1);
        if (date.getDayOfWeek() == DayOfWeek.THURSDAY) {
          if (PracticeUtils.getOrdinalDayOfWeek(date) == 3 && !legacyEnabled) {
            date = date.minusWeeks(1);
          }
          return date.toString();
        }
      }

    case "current":
    case "today":
    case "this":
      return date.toString();

    case "next":
      while (true) {
        date = date.plusDays(1);
        if (date.getDayOfWeek() == DayOfWeek.THURSDAY) {
          if (PracticeUtils.getOrdinalDayOfWeek(date) == 3 && !legacyEnabled) {
            date = date.plusWeeks(1);
          }
          return date.toString();
        }
      }

    default:
      return s;
    }
  }
}
