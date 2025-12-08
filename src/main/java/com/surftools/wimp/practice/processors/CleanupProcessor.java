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

package com.surftools.wimp.practice.processors;

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
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class CleanupProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(CleanupProcessor.class);
  private String dateString = null;
  private boolean isFinalizing = false;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    dateString = cm.getAsString(Key.EXERCISE_DATE);
    isFinalizing = cm.getAsBoolean(Key.PRACTICE_ENABLE_FINALIZE);
  }

  @Override
  public void process() {
  }

  @Override
  public void postProcess() {
    // move unused files
    var unusedDirPath = Path.of(outputPath.toString(), "unused");
    var unusedDirName = unusedDirPath.toString();
    FileUtils.deleteDirectory(unusedDirPath);
    FileUtils.makeDirIfNeeded(unusedDirPath.toString());

    var outputDir = new File(outputPath.toString());
    var outputFiles = outputDir.listFiles();

    for (var outputFile : outputFiles) {
      if (!outputFile.isFile()) {
        continue;
      }

      var name = outputFile.getName();
      if (name.equals("practice-summary.csv")) {
        try {
          Files.move(outputFile.toPath(), Path.of(outputPathName, dateString + "-standard-summary.csv"),
              StandardCopyOption.ATOMIC_MOVE);
          logger.info("renamed practice-summary.csv" + name + " to: " + dateString + "-summary.csv");
        } catch (Exception e) {
          logger.error("Exception renaming file: " + outputFile.toString(), e.getMessage());
        }
      } else if (name.endsWith(".csv")) {
        try {
          Files.move(outputFile.toPath(), Path.of(unusedDirName, name), StandardCopyOption.ATOMIC_MOVE);
          logger.info("moved output/" + name + " to usused/");
        } catch (Exception e) {
          logger.error("Exception moving file: " + outputFile.toString(), e.getMessage());
        }
      }
    } // end loop over files

    // copy allFeedback.txt to usused
    var allFeedbackSource = Path.of(pathName.toString(), "allFeedback.txt");
    var allFeedbackDestination = Path.of(unusedDirName, "allFeedback.txt");
    try {
      Files.copy(allFeedbackSource, allFeedbackDestination, StandardCopyOption.REPLACE_EXISTING);
      logger.info("copied allFeedback.txt to unused/");
    } catch (Exception e) {
      logger.error("Exception copying file: " + allFeedbackSource.toString(), e.getMessage());
    }

    // rename chart, map and Winlink import files
    outputFiles = outputDir.listFiles();
    for (var outputFile : outputFiles) {
      if (!outputFile.isFile()) {
        continue;
      }

      var name = outputFile.getName();
      if (name.endsWith("chart.html")) {
        try {
          var newFile = Path.of(outputPathName, dateString + "-chart.html").toFile();
          outputFile.renameTo(newFile);
          logger.info("renamed chart file to: " + newFile.getName());
        } catch (Exception e) {
          logger.error("Exception renaming file: " + name, e.getMessage());
        }
      }

      if (name.startsWith("leaflet-ETO Weekly Practice for ")) {
        try {
          var newFile = Path.of(outputPathName, dateString + "-map.html").toFile();
          outputFile.renameTo(newFile);
          logger.info("renamed map file to: " + newFile.getName());
        } catch (Exception e) {
          logger.error("Exception renaming file: " + name, e.getMessage());
        }
      } else if (name.startsWith("leaflet")) {
        try {
          var newName = name.replace("leaflet", dateString + "-map");
          var newFile = Path.of(unusedDirName, newName).toFile();
          outputFile.renameTo(newFile);
          logger.info("renamed map file to: " + newFile.getName());
        } catch (Exception e) {
          logger.error("Exception renaming file: " + name, e.getMessage());
        }
      }

      if (name.startsWith("all-winlink")) {
        try {
          var newFile = Path.of(outputPathName, "Winlink_ImportMessages-" + dateString + ".xml").toFile();
          outputFile.renameTo(newFile);
          logger.info("renamed Winlink import file to: " + newFile.getName());
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
              Files.move(outputFile.toPath(), Path.of(unusedDirName, name), StandardCopyOption.ATOMIC_MOVE);
              logger.info("moved Winlink import file: " + name + "  to unused");
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

      var finalPath = Path.of(outputPath + "-FINAL-" + timestamp);
      FileUtils.copyDirectory(outputPath, finalPath);
      logger.info("copied output dir to: " + finalPath.toString());
    }

  } // end postProcess

}
