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

package com.surftools.wimp.processors.std;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.practice.tools.PracticeProcessorTool;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class CleanupProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(CleanupProcessor.class);
  private String dateString = null;
  private boolean isFinalizing = false;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    dateString = cm.getAsString(Key.EXERCISE_DATE);
    isFinalizing = cm.getAsBoolean(Key.ENABLE_FINALIZE);
  }

  @Override
  public void process() {
  }

  @Override
  public void postProcess() {

    // move files out of output and into published
    var outputDir = new File(outputPath.toString());
    var outputFiles = outputDir.listFiles();
    for (var file : outputFiles) {
      if (!file.isFile()) {
        continue;
      }

      var fileName = file.getName();
      if (fileName.equals("practice-summary.csv")) {
        try {
          Files.move(file.toPath(), Path.of(publishedPathName, dateString + "-standard-summary.csv"));
          logger.info("renamed practice-summary.csv" + fileName + " to: " + dateString + "-standard-summary.csv");
        } catch (Exception e) {
          logger.error("Exception renaming file: " + file.toString(), e.getMessage());
        }
      }
    } // end loop over files

    // copy input/allFeedback.txt to output
    var allFeedbackSource = Path.of(inputPathName, "allFeedback.txt");
    var allFeedbackDestination = Path.of(outputPathName, dateString + "-allFeedback.txt");
    try {
      Files.copy(allFeedbackSource, allFeedbackDestination);
      logger.info("copied allFeedback.txt to output/");
    } catch (Exception e) {
      logger.error("Exception copying file: " + allFeedbackSource.toString(), e.getMessage());
    }

    // copy configurationFile to input
    var configurationFileSource = Path.of((String) mm.getContextObject(PracticeProcessorTool.CONFIGURATION_FILE_KEY));
    var configurationFileDestination = Path.of(inputPathName, "configuration.txt");
    try {
      Files.copy(configurationFileSource, configurationFileDestination, StandardCopyOption.REPLACE_EXISTING);
      logger.info("copied configuration to input/");
    } catch (Exception e) {
      logger.error("Exception copying file: " + configurationFileSource.toString(), e.getMessage());
    }

    // rename published files chart, map and Winlink import files
    var publishedDir = new File(publishedPath.toString());
    var publishedFiles = publishedDir.listFiles();
    for (var publishedFile : publishedFiles) {
      if (!publishedFile.isFile()) {
        continue;
      }

      var name = publishedFile.getName();
      if (name.endsWith("chart.html")) {
        try {
          var newFile = Path.of(publishedPathName, dateString + "-chart.html").toFile();
          publishedFile.renameTo(newFile);
          logger.info("renamed chart file to: " + newFile.getName());
        } catch (Exception e) {
          logger.error("Exception renaming file: " + name, e.getMessage());
        }
      }

      if (name.startsWith("leaflet-ETO Weekly Practice for ")) {
        try {
          var newFile = Path.of(publishedPathName, dateString + "-map.html").toFile();
          publishedFile.renameTo(newFile);
          logger.info("renamed map file to: " + newFile.getName());
        } catch (Exception e) {
          logger.error("Exception renaming file: " + name, e.getMessage());
        }
      }
    } // end rename loop over files

    // because I'd rather write 50 lines of code than point and click to import two
    // files into Winlink Express ...
    try {
      outputFiles = outputDir.listFiles();
      var messageLines = new StringBuffer();
      for (var outputFile : outputFiles) {
        if (!outputFile.isFile()) {
          continue;
        }

        var name = outputFile.getName();
        if (name.endsWith(".xml")) {
          var lines = Files.readAllLines(Path.of(outputFile.getCanonicalPath()));
          var inMessages = false;
          for (var line : lines) {
            var testLine = line.toLowerCase().trim();
            if (!inMessages && testLine.equals("<message_list>")) {
              inMessages = true;
              continue;
            }
            if (inMessages && testLine.equals("</message_list>")) {
              break; // loop over lines
            }
            if (inMessages) {
              messageLines.append(line + "\n");
            }
          } // end loop over lines
        } // end if .xml file
      } // end merge outbound message merge loop over files

      if (messageLines.length() > 0) {
        final String header = """
            <?xml version="1.0"?>
            <Winlink_Express_message_export>
              <export_parameters>
                <xml_file_version>1.0</xml_file_version>
                <winlink_express_version>1.7.24.0</winlink_express_version>
              </export_parameters>
              <message_list>
              """;

        final String footer = """
              </message_list>
            </Winlink_Express_message_export>
            """;

        messageLines.insert(0, header);
        messageLines.append(footer);
        var path = Path.of(outputPathName, dateString + "-Winlink-Import-All-Messages.xml");

        Files.writeString(path, messageLines.toString());
        logger.info("created merged Winlink import file: " + path.toFile().getName());
      }
    } catch (Exception e) {
      logger.error("Exception merging Winlink message files: " + e.getMessage());
    }

    if (isFinalizing) {
      var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
      LocalDateTime now = LocalDateTime.now();
      var timestamp = now.format(formatter);

      var tempPath = Path.of(exercisesPathName, "FINAL-" + timestamp);
      FileUtils.copyDirectory(exercisePath, tempPath);
      var tempDir = tempPath.toFile();
      var finalDir = Path.of(exercisePathName, "FINAL-" + timestamp).toFile();
      tempDir.renameTo(finalDir);
      logger.info("copied exercise dir to: " + finalDir.toString());
    }

  } // end postProcess

}
