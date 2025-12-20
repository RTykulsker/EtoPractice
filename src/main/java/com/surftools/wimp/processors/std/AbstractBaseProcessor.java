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

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

public abstract class AbstractBaseProcessor implements IProcessor {
  protected Logger logger = LoggerFactory.getLogger(AbstractBaseProcessor.class);

  protected static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  protected static final DateTimeFormatter ALT_DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING.replaceAll("-", "/"));
  protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  protected static IConfigurationManager cm;
  protected static IMessageManager mm;

  public static String exercisesPathName;
  public static Path exercisesPath;
  public static String exercisePathName;
  public static Path exercisePath;
  public static String inputPathName;
  public static Path inputPath;
  public static String outputPathName;
  public static Path outputPath;
  public static String publishedPathName;
  public static Path publishedPath;

  protected static String dateString;
  protected static LocalDate date;

  protected static boolean isInitialized = false;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    if (!isInitialized) {
      doInitialization(cm, mm);
      isInitialized = true;
    }
  }

  protected void doInitialization(IConfigurationManager _cm, IMessageManager _mm) {
    cm = _cm;
    mm = _mm;

    dateString = cm.getAsString(Key.EXERCISE_DATE);
    date = LocalDate.parse(dateString);
    var exerciseYear = date.getYear();
    var exerciseYearString = String.valueOf(exerciseYear);

    exercisesPathName = cm.getAsString(Key.PATH_EXERCISES);
    exercisesPath = Path.of(exercisesPathName);
    exercisePath = Path.of(exercisesPathName, exerciseYearString, dateString);
    exercisePathName = exercisePath.toString();

    // already created in the tool, so Winlink Express export can put to right place
    inputPath = Path.of(exercisePathName, "input");
    inputPathName = inputPath.toString();

    outputPath = Path.of(exercisePathName, "output");
    // don't delete here, already deleted in PracticeProcessorTool, FileUtils.deleteDirectory(outputPath);
    FileUtils.makeDirIfNeeded(outputPath.toString());
    outputPathName = outputPath.toString();

    publishedPath = Path.of(exercisePathName, "published");
    FileUtils.deleteDirectory(publishedPath);
    FileUtils.makeDirIfNeeded(publishedPath.toString());
    publishedPathName = publishedPath.toString();
  }

  @Override
  public abstract void process();

  @Override
  public void postProcess() {
  }
}
