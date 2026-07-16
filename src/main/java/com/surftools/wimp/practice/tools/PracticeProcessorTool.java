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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Stream;

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
import com.surftools.wimp.schedule.ScheduleCheckResult;
import com.surftools.wimp.schedule.ScheduleManager;
import com.surftools.wimp.schedule.ScheduleRecord;
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

  private ScheduleCheckResult checkResult;
  private ScheduleRecord scheduleRecord;

  public static void main(String[] args) {
    var tool = new PracticeProcessorTool();
    CmdLineParser parser = new CmdLineParser(tool);
    try {
      parser.parseArgument(args);
      tool.run();
    } catch (Exception e) {
      e.printStackTrace(System.err);
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

      var ord = PracticeUtils.getOrdinalDayOfWeek(exerciseDate);
      var messageType = scheduleRecord.messageType();
      if (messageType == null) {
        logger.error("null type for: " + scheduleRecord.date() + ", name: " + scheduleRecord.name() + ", extra: "
            + scheduleRecord.extraData());
        System.exit(1);
      }

      var dayOfWeek = scheduleRecord.date().getDayOfWeek().toString();
      logger.info("Exercise Date: " + exerciseDate.toString() + ", " + PracticeUtils.getOrdinalLabel(ord) + " "
          + dayOfWeek + ",  exercise message type: " + messageType.toString());

      var exercisesPathName = cm.getAsString(Key.PATH_EXERCISES);
      logger.info("exercises home" + exercisesPathName);

      // fail fast on reading reference
      var referencePathName = cm.getAsString(Key.PATH_REFERENCE);
      logger.info("reference home: " + referencePathName);
      var exerciseYearString = String.valueOf(exerciseDate.getYear());
      var referencePath = Path.of(referencePathName, exerciseYearString, exerciseDateString,
          exerciseDateString + "-reference.json");
      copyReferenceFilesToInputIfNeeded(referencePath, exerciseYearString, cm);
      var jsonString = Files.readString(referencePath);
      var deserializer = new PracticeJsonMessageDeserializer();
      var referenceMessage = deserializer.deserialize(jsonString, messageType);

      // make exercises folder if needed
      FileUtils.createDirectory(Path.of(exercisesPathName, exerciseYearString, exerciseDateString));
      FileUtils.createDirectory(Path.of(exercisesPathName, exerciseYearString, exerciseDateString, "input"));

      var winlinkCallsign = cm.getAsString(Key.WINLINK_CALLSIGN);
      logger.info("Winlink callsign: " + winlinkCallsign);

      final var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

      var instructionText = "";
      var nextSchedule = checkResult.nextOutput();
      if (nextSchedule != null) {
        var nextExerciseDate = nextSchedule.date();
        if (nextExerciseDate == null) {
          instructionText = "No instructions for next exercise are currently available";
        } else {
          var sb = new StringBuilder();
          if (!nextSchedule.isPractice()) {
            var text = """
                --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

                INSTRUCTIONS for next week:
                Next Thursday is a \"Third Thursday Training Exercise\".
                These instructions are simply too large for a Winlink messages,
                so look for instructions on our web site at https://emcomm-training.org/Winlink_Thursdays.html
                            """;
            sb.append(text);
          } else {
            var nextExerciseYear = nextExerciseDate.getYear();
            var nextExerciseDateString = dtf.format(nextExerciseDate);
            var instructionPath = Path.of(referencePathName, String.valueOf(nextExerciseYear), nextExerciseDateString,
                nextExerciseDateString + "-instructions.txt");
            instructionText = Files.readString(instructionPath);
            sb.append("\n\n");
          }
          instructionText = sb.toString();
        } // end if normal practice
      } // end if nextSchedule != null

      // create the rest of our configuration on the fly
      cm.putString(Key.EXERCISE_DATE, exerciseDateString);
      cm.putString(Key.EXERCISE_NAME, "ETO Weekly Practice for " + exerciseDateString);

      cm.putString(Key.EXPECTED_MESSAGE_TYPES, messageType.toString());

      var windowOpenDate = exerciseDate.minusDays(5);
      cm.putString(Key.EXERCISE_WINDOW_OPEN, dtf.format(windowOpenDate) + " 00:00");
      var windowCloseDate = exerciseDate.plusDays(1);
      cm.putString(Key.EXERCISE_WINDOW_CLOSE, dtf.format(windowCloseDate) + " 08:00");

      cm.putString(Key.PIPELINE_STDIN, "Read,Classifier,Acknowledgement,Deduplication");
      cm.putString(Key.PIPELINE_MAIN, messageType.getPracticeProcessorName());
      cm.putString(Key.PIPELINE_STDOUT, "Write,HistoryMap,ExerciseSummary,ParticipantHistory,Cleanup,Finalize");

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

  /**
   * copy all files in the referencePath (for the given exerciseDate) to input
   *
   * this allows for a durable copy (after finalization) if reference is
   * regenerated
   *
   * @param referenceFilePath
   * @param exerciseYearString
   * @param cm
   */
  private void copyReferenceFilesToInputIfNeeded(Path referenceFilePath, String exerciseYearString,
      IConfigurationManager cm) {
    var parentPath = referenceFilePath.getParent();
    var inputPath = Path.of(cm.getAsString(Key.PATH_EXERCISES), exerciseYearString, exerciseDateString);
    try (Stream<Path> stream = Files.list(parentPath)) {
      var refFiles = stream.filter(Files::isRegularFile).toList();
      for (var refFile : refFiles) {
        var fileName = refFile.getFileName().toString();
        var inputFilePath = Path.of(inputPath.toString(), fileName);
        var inputFile = inputFilePath.toFile();
        if (inputFile.exists()) {
          logger.info("reference file: " + fileName + " already exists in input, skipping");
        } else {
          Files.copy(refFile, inputFilePath);
          logger.info("reference file: " + fileName + " copied to input");
        }
      }
    } catch (Exception e) {
      logger.error(
          "Exception copying reference files for: " + referenceFilePath.toString() + ", " + e.getLocalizedMessage());
    }
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
      logger.error("Can not parse exerciseDate: " + input);
      System.exit(1);
    }

    var date = LocalDate.now();

    final var knownSet = Set.of("last", "current", "today", "this", "next");
    var s = input.toLowerCase();
    if (!knownSet.contains(s)) {
      try {
        date = LocalDate.parse(input);
      } catch (Exception e) {
        logger.error("Can not parse exerciseDate: " + input);
        System.exit(1);
      }
    }
    var scheduleManager = new ScheduleManager(cm);
    checkResult = scheduleManager.check(date);

    if (s.equals("last")) {
      if (checkResult.lastOutput() == null) {
        logger.error("No exercise scheduled for: " + input);
        System.exit(1);
      }
      scheduleRecord = checkResult.lastOutput();
    } else if (s.equals("next")) {
      if (checkResult.nextOutput() == null) {
        logger.error("No exercise scheduled for: " + input);
        System.exit(1);
      }
      scheduleRecord = checkResult.nextOutput();
    } else {
      if (checkResult.thisOuput() == null) {
        logger.error("No exercise scheduled for: " + input);
        System.exit(1);
      }
      scheduleRecord = checkResult.thisOuput();
    }

    return scheduleRecord.date().toString();
  }
}
