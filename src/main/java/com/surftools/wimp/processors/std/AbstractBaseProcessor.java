/**

The MIT License (MIT)

Copyright (c) 2022, Robert Tykulsker

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

public abstract class AbstractBaseProcessor implements IProcessor {
  protected static Logger logger;

  protected static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  protected static final DateTimeFormatter ALT_DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING.replaceAll("-", "/"));
  protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  protected static IConfigurationManager cm;
  protected static IMessageManager mm;

  protected static String pathName;
  protected static String outputPathName;
  protected static Path outputPath;

  protected static boolean isInitialized = false;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    initialize(cm, mm, LoggerFactory.getLogger(AbstractBaseProcessor.class));
  }

  public void initialize(IConfigurationManager cm, IMessageManager mm, Logger _logger) {
    logger = _logger;
    if (!isInitialized) {
      doInitialization(cm, mm);
      isInitialized = true;
    }
  }

  protected static void uninitialize() {
    isInitialized = false;
  }

  protected void doInitialization(IConfigurationManager _cm, IMessageManager _mm) {
    cm = _cm;
    mm = _mm;

    pathName = cm.getAsString(Key.PATH);
    // fail fast: our working directory, where our input files are
    Path path = Paths.get(pathName);
    if (!Files.exists(path)) {
      logger.error("specified path: " + pathName + " does not exist");
      System.exit(1);
    } else {
      logger.info("Starting with input path: " + path);
    }

    // allow overriding of outputPathName!
    outputPathName = cm.getAsString(Key.OUTPUT_PATH);
    if (outputPathName == null) {
      outputPath = Path.of(path.toAbsolutePath().toString(), "output");
      outputPathName = outputPath.toString();
      logger.info("outputPath: " + outputPath);
    } else {
      outputPath = Path.of(outputPathName);
    }

    if (cm.getAsBoolean(Key.OUTPUT_PATH_CLEAR_ON_START, true)) {
      FileUtils.deleteDirectory(outputPath);
    }
    FileUtils.makeDirIfNeeded(outputPath.toString());

  }

  @Override
  public abstract void process();

  @Override
  public void postProcess() {
  }
}
