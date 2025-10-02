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
import java.util.List;

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

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    dateString = cm.getAsString(Key.EXERCISE_DATE);
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

    var knownUnusedList = List.of("feedback.kml", "acknowledgment-winlinkExpressOutboundMessages.xml");
    for (var outputFile : outputFiles) {
      if (!outputFile.isFile()) {
        continue;
      }

      var name = outputFile.getName();
      if (name.endsWith(".csv") || knownUnusedList.contains(name)) {
        try {
          Files.move(outputFile.toPath(), Path.of(unusedDirName, name), StandardCopyOption.ATOMIC_MOVE);
          logger.info("moved output/" + name + " to usused/");
        } catch (Exception e) {
          logger.error("Exception moving file: " + outputFile.toString(), e.getMessage());
        }
      }
    }

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
          var newFile = Path.of(outputPathName, "chart-" + dateString + ".html").toFile();
          outputFile.renameTo(newFile);
          logger.info("renamed chart file to: " + newFile.getName());
        } catch (Exception e) {
          logger.error("Exception renaming file: " + name, e.getMessage());
        }
      }

      if (name.startsWith("leaflet")) {
        try {
          var newFile = Path.of(outputPathName, "map-" + dateString + ".html").toFile();
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
    }
  }

}
