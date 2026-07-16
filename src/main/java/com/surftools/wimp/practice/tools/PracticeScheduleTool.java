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

import java.io.FileInputStream;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

  private static final LocalDate startDate = LocalDate.of(2025, 1, 1);
  private static LocalDate endDate;
  private static final String SHEET_SCHEDULE = "schedule";
  private static final String SHEET_OVERRIDE = "override";
  private static final String SHEET_PROHIBITED = "prohibited";
  private static final List<String> REQUIRED_SHEET_NAMES = List.of(SHEET_SCHEDULE, SHEET_OVERRIDE, SHEET_PROHIBITED);

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
    logger.info("startDate: " + startDate.toString());
    endDate = LocalDate.of(startDate.getYear() + nYears, 12, 31);
    logger.info("endDate: " + endDate.toString());

    var metaScheduleFileName = cm.getAsString(Key.PATH_META_SCHEDULE);

    var sheetMap = processExcelFile(metaScheduleFileName, rng);
    var outputList = generateSchedule(startDate, endDate, sheetMap);

    var schedulePathString = cm.getAsString(Key.PATH_SCHEDULE);
    var schedulePath = Path.of(schedulePathString);
    var scheduleFile = schedulePath.toFile();

    if (scheduleFile.exists()) {
      var scheduleParentPath = schedulePath.getParent();
      var historyPath = Path.of(scheduleParentPath.toString(), "schedule-history");
      FileUtils.makeDirIfNeeded(historyPath);
      var scheduleAttributes = Files.readAttributes(schedulePath, BasicFileAttributes.class);
      FileTime creationTime = scheduleAttributes.lastModifiedTime();
      var instant = creationTime.toInstant();
      var dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
      var timeStamp = String.format("%02d%02d%02d-%02d%02d%02d", //
          dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(), //
          dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());
      var destinationPath = Path.of(historyPath.toString(), "schedule-" + timeStamp + ".csv");
      Files.move(schedulePath, destinationPath, StandardCopyOption.ATOMIC_MOVE);
    }

    WriteProcessor.writeTable(new ArrayList<IWritableTable>(outputList), schedulePath);

    var today = LocalDate.now();
    var scheduleManager = new ScheduleManager(cm);
    var searchResult = scheduleManager.check(today);
    logger.info("searchResult: " + searchResult);
    logger.info("end run");
  }

  private Map<String, List<InternalRecord>> processExcelFile(String metaScheduleFileName, Random rng) {
    logger.info("processing Excel file: " + metaScheduleFileName);
    var map = new HashMap<String, List<InternalRecord>>();
    var path = Path.of(metaScheduleFileName);
    var file = path.toFile();
    var startYear = startDate.getYear();
    var endYear = endDate.getYear();

    var messageTypeListChooserMap = new HashMap<ArrayList<MessageType>, BucketChooser<MessageType>>();

    try (var fis = new FileInputStream(file);
        var workbook = file.getName().endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

      for (var sheetName : REQUIRED_SHEET_NAMES) {
        var list = new ArrayList<InternalRecord>();
        var sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
          logger.error("sheet: " + sheetName + " not found in file: " + metaScheduleFileName);
          System.exit(1);
        }

        logger.info("processing sheet: " + sheet.getSheetName());

        int rowNumber = 0;
        for (var row : sheet) {
          ++rowNumber;
          if (rowNumber == 1) { // skip header row
            continue;
          }

          var cnp = "sheet: " + sheetName + ", row: " + rowNumber + ", could not parse ";
          var name = getStringValue(row, 0);

          var extraData = getStringValue(row, 6);

          var ordinalString = getStringValue(row, 1);
          int ordinal = -1;
          try {
            var d = Double.parseDouble(ordinalString);
            ordinal = (int) d;
          } catch (Exception e) {
            logger.error(cnp + "ordinal: " + ordinalString + ", " + e.getMessage());
            System.exit(1);
          }

          var dowString = getStringValue(row, 2);
          var dow = DayOfWeek.valueOf(dowString.toUpperCase());
          if (dow == null) {
            logger.error(cnp + "Day of Week: " + dowString);
            System.exit(1);
          }

          var month = (Month) null;
          var monthString = getStringValue(row, 3);
          if (monthString != null && monthString.strip().length() > 0) {
            try {
              month = Month.valueOf(monthString);
              if (month == null) {
                logger.error(cnp + "Month: " + month);
                System.exit(1);
              }
            } catch (Exception e) {
              logger.error(cnp + "Month: " + month);
              System.exit(1);
            }
          }

          var yearString = getStringValue(row, 4);
          Integer year = null;
          if (yearString != null && yearString.strip().length() > 1) {
            try {
              var d = Double.parseDouble(yearString);
              year = (int) d;

              if (year < startYear) {
                logger.error(cnp + "year: " + yearString + " must be before: " + startYear);
                System.exit(1);
              }

              if (year > endYear) {
                logger.error(cnp + "year: " + yearString + " must be after: " + endYear);
                System.exit(1);
              }
            } catch (Exception e) {
              logger.error(cnp + "year: " + yearString + ", " + e.getMessage());
              System.exit(1);
            }
          }

          var messageTypesString = getStringValue(row, 5);
          BucketChooser<MessageType> chooser = null;
          if (sheetName != SHEET_PROHIBITED) {
            var messageTypeList = new ArrayList<MessageType>();
            var fields = messageTypesString.split(",");
            for (var field : fields) {
              field = field.strip().toUpperCase();
              var messageType = MessageType.valueOf(field);
              if (messageType == null) {
                logger.error(cnp + "messageType: " + field + ", not a MessageType");
                System.exit(1);
              } else {
                messageTypeList.add(messageType);
              }
            } // end loop over fields
            Collections.sort(messageTypeList);
            chooser = messageTypeListChooserMap.get(messageTypeList);
            if (chooser == null) {
              chooser = new BucketChooser<MessageType>(messageTypeList, rng);
              messageTypeListChooserMap.put(messageTypeList, chooser);
            }
          } // end if not prohibited sheet

          var isPractice = !sheetName.equals(SHEET_PROHIBITED);
          var internalRecord = new InternalRecord(name, ordinal, dow, month, year, chooser, isPractice, extraData);
          list.add(internalRecord);

        } // end loop over rows in sheet
        logger.info("read: " + list.size() + " rows from sheet: " + sheetName);
        map.put(sheetName, list);
      } // end loop over sheets in workbook
    } catch (Exception e) {
      logger.error("Exception processing Excel file: " + file.getPath() + ", " + e.getMessage());
      e.printStackTrace();
    }

    return map;
  }

  protected String getStringValue(Row row, int columnIndex) {
    Cell cell = row.getCell(columnIndex);
    if (cell == null) {
      return "";
    }

    switch (cell.getCellType()) {
    case BLANK:
      return "";

    case BOOLEAN:
      return Boolean.toString(cell.getBooleanCellValue());

    case FORMULA: {
      CellType cachedCellType = cell.getCachedFormulaResultType();
      if (cachedCellType == CellType.STRING) {
        return cell.getStringCellValue().strip();
      } else if (cachedCellType == CellType.NUMERIC) {
        return Double.toString(cell.getNumericCellValue());
      } else if (cachedCellType == CellType.BOOLEAN) {
        return Boolean.toString(cell.getBooleanCellValue());
      }
    }

    case NUMERIC:
      return Double.toString(cell.getNumericCellValue());

    case STRING:
      return cell.getStringCellValue().strip();

    default:
      logger.error(
          "Unsupported type: " + cell.getCellType().name() + " on row: " + row.getRowNum() + ", col: " + columnIndex);
      return "";
    }
  }

  protected List<ScheduleRecord> generateSchedule(LocalDate startDate, LocalDate endDate,
      Map<String, List<InternalRecord>> inputMap) {

    var matches = new ArrayList<InternalRecord>();
    var dateScheduleRecordMap = new HashMap<LocalDate, ScheduleRecord>();

    for (var sheetName : REQUIRED_SHEET_NAMES) {
      var inputList = inputMap.get(sheetName);
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
        } // end loop over inputList

        if (matches.size() > 1) {
          throw new RuntimeException("Multiple IR matches for date: " + date.toString());
        }

        if (matches.size() == 1) {
          var input = matches.get(0);
          var chooser = input.chooser;
          var messageType = (sheetName.equals(SHEET_PROHIBITED)) ? null : chooser.next();
          var output = new ScheduleRecord(input.name, date, messageType, input.isPractice, input.extraData);

          var previousOutput = dateScheduleRecordMap.get(date);
          if (previousOutput != null) {
            logger.info("Phase: " + sheetName + ", overriding: " + previousOutput + ", with: " + output);
          }
          dateScheduleRecordMap.put(date, output);
        }

        date = date.plusDays(1);
      } // end loop over days
    } // end loop over sheets and overriding

    var outputList = new ArrayList<ScheduleRecord>(dateScheduleRecordMap.values());
    Collections.sort(outputList);
    return outputList;
  }

  record InternalRecord(String name, int ordinalDayOfWeek, DayOfWeek dayOfWeek, Month month, Integer year,
      BucketChooser<MessageType> chooser, boolean isPractice, String extraData) {
  }
}
