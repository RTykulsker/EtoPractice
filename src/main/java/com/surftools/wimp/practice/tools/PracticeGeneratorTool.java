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

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.surftools.utils.BucketChooser;
import com.surftools.utils.ExcelUtils;
import com.surftools.utils.FileUtils;
import com.surftools.wimp.configuration.GenKey;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.generator.IGenerator;
import com.surftools.wimp.generator.PracticeUtils;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.schedule.ScheduleRecord;
import com.surftools.wimp.service.email.EmailService;
import com.surftools.wimp.utils.config.IConfigurationManager;
import com.surftools.wimp.utils.config.impl.PropertyFileConfigurationManager;

/**
 * 2nd Generation tool to generate exercise, combining previous
 */
public class PracticeGeneratorTool {
	private static final Logger logger = LoggerFactory.getLogger(PracticeGeneratorTool.class);

	private static final String SHEET_SCHEDULE = "schedule";
	private static final String SHEET_OVERRIDE = "override";
	private static final String SHEET_PROHIBITED = "prohibited";
	private static final List<String> REQUIRED_SHEET_NAMES = List.of(SHEET_SCHEDULE, SHEET_OVERRIDE, SHEET_PROHIBITED);

	static {
		System.setProperty("logback.configurationFile", "resources/logback.xml");
	}

	@Option(name = "--config", usage = "practice configuration file name", required = true)
	private String configurationFileName;

	@Option(name = "--enableFinalize", usage = "to rename output,  email to ETO folks upon completion", required = false)
	private boolean enableFinalize = false;

	private IConfigurationManager cm;

	private String timestampString;
	private LocalDateTime now;
	private LocalDate startDate;
	private LocalDate endDate;
	private LocalDate legacyDate;

	private String generationPathString; // path string to top-level of where we write stuff
	private Path generationPath; // path to top-level of where we write stuff
	private Path instructionsPath; // path to where we always write instructions
	private Path newInstructionsPath; // path to where we conditionally write instructions
	private Path oldReferencePath;
	private String oldReferencePathString;
	private String commitMessage;

	private Random rng;

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

		initialize();
		var schedule = makeSchedule();
		generateExercises(schedule);

		if (enableFinalize) {
			doFinalization();
		}

		logger.info("end run");
	}

	private void initialize() throws Exception {
		cm = new PropertyFileConfigurationManager(configurationFileName, GenKey.values());

		var timestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
		now = LocalDateTime.now();
		timestampString = timestampFormatter.format(now);

		logger.info("enableFinalize: " + enableFinalize);
		logger.info("timestamp: " + timestampString);

		var referencePathString = cm.getAsString(GenKey.PATH_REFERENCE);
		var referencePath = Path.of(referencePathString);
		logger.info("referencePath: " + referencePath.toString());

		var metaScheduleFileName = cm.getAsString(GenKey.PATH_META_SCHEDULE);
		var metaScheduleFile = new File(metaScheduleFileName);
		if (!metaScheduleFile.exists()) {
			logger.error(GenKey.PATH_META_SCHEDULE + " file: " + metaScheduleFileName + " not found. Exiting!");
			System.exit(1);
		}

		var rngSeedString = cm.getAsString(GenKey.GENERATOR_RNG_SEED, "2025");
		var rngSeed = Long.valueOf(rngSeedString);
		logger.info("rngSeed: " + rngSeed);
		rng = new Random(rngSeed);

		oldReferencePathString = cm.getAsString(GenKey.PATH_REFERENCE);
		oldReferencePath = Path.of(oldReferencePathString);
		var oldReferenceDir = oldReferencePath.toFile();
		if (!oldReferenceDir.exists()) {
			logger.error(GenKey.PATH_REFERENCE + ": " + oldReferencePathString + " doesn't exist. Exiting!");
			System.exit(1);
		}

		getCommitMessage();
	}

	private void getCommitMessage() throws Exception {
		var commitMessagePathString = cm.getAsString(GenKey.PATH_COMMIT_MESSAGE);
		commitMessage = Files.readString(Path.of(commitMessagePathString));
		logger.info("commitMessage: " + commitMessage);
		var commitSha1Hash = DigestUtils.sha1Hex(commitMessage);

		var oldHistoryPath = Path.of(oldReferencePathString, "publication-history");
		var oldHistoryDir = oldHistoryPath.toFile();
		var files = oldHistoryDir.listFiles();

		for (var file : files) {
			var path = file.toPath();
			var fileContents = Files.readString(path);
			var fileSha1Hash = DigestUtils.sha1Hex(fileContents);
			if (commitSha1Hash.equals(fileSha1Hash)) {
				logger.error("### commit message is not unique. Collides with: " + path.toString() + ". Exiting!");
				System.exit(1);
			}
		}
	}

	private List<ScheduleRecord> makeSchedule() throws Exception {
		var startDateString = cm.getAsString(GenKey.GENERATOR_START_DATE, "2025-01-01");
		startDate = LocalDate.parse(startDateString);
		logger.info("startDate: " + startDate.toString());

		var nYears = cm.getAsInt(GenKey.GENERATOR_N_YEARS, 5);
		logger.info("nYears: " + nYears);

		endDate = startDate.plusYears(nYears);
		logger.info("endDate: " + endDate);

		var legecyDateString = cm.getAsString(GenKey.GENERATOR_LEGACY_DATE);
		if (legecyDateString == null || legecyDateString.strip().isEmpty()) {
			logger.error(GenKey.GENERATOR_LEGACY_DATE.toString() + " must be provided");
			System.exit(1);
		}

		legacyDate = LocalDate.parse(legecyDateString);
		logger.info("legacyDate: " + legacyDate.toString());

		var historyPathString = cm.getAsString(GenKey.PATH_REFERENCE_HISTORY);
		var historyPath = Path.of(historyPathString);
		var historyDir = historyPath.toFile();
		if (!historyDir.exists() || !historyDir.isDirectory()) {
			logger.error(GenKey.PATH_REFERENCE_HISTORY.toString() + " doesn't exist or isn't a directory. Exiting!");
			System.exit(1);
		}

		var genType = enableFinalize ? "published" : "generated";
		generationPath = Path.of(historyPathString, "reference-" + genType + "-" + timestampString);
		generationPathString = generationPath.toString();

		makeDirectories();

		var metaSchedulePath = Path.of(cm.getAsString(GenKey.PATH_META_SCHEDULE));
		Files.copy(metaSchedulePath, Path.of(generationPathString, metaSchedulePath.getFileName().toString()));

		var metaScheduleFileName = cm.getAsString(GenKey.PATH_META_SCHEDULE);

		var sheetMap = processExcelFile(metaScheduleFileName, rng);
		var outputList = generateSchedule(startDate, endDate, sheetMap);

		var schedulePath = Path.of(generationPathString, "schedule.csv");
		WriteProcessor.writeTable(new ArrayList<IWritableTable>(outputList), schedulePath);

		return outputList;
	}

	private void makeDirectories() throws Exception {
		logger.info("generating exercises in " + generationPathString);
		FileUtils.makeDirIfNeeded(generationPath);

		Files.copy(Path.of(cm.getAsString(GenKey.PATH_COMMIT_MESSAGE)),
				Path.of(generationPathString, "commit-message.txt"));

		Files.copy(Path.of(cm.getAsString(GenKey.PATH_ROOT), configurationFileName),
				Path.of(generationPathString, configurationFileName.split("/")[1]));

		instructionsPath = Path.of(generationPathString, "instructions");
		FileUtils.makeDirIfNeeded(instructionsPath);

		newInstructionsPath = Path.of(generationPathString, "new-instructions");
		FileUtils.makeDirIfNeeded(newInstructionsPath);

		var startYear = startDate.getYear();
		var endYear = endDate.getYear();
		var legacyYear = legacyDate.getYear();

		for (var year = startYear; year <= endYear; ++year) {
			var yearString = String.valueOf(year);
			FileUtils.makeDirIfNeeded(generationPath, yearString);
			FileUtils.makeDirIfNeeded(instructionsPath, yearString);
			if (year >= legacyYear) {
				FileUtils.makeDirIfNeeded(newInstructionsPath, yearString);
			}
		}
	}

	private void generateExercises(List<ScheduleRecord> scheduleList) throws Exception {
		var generatorMap = new HashMap<MessageType, IGenerator>();
		for (var type : MessageType.getAllSupportedTypes()) {
			var generatorName = "com.surftools.wimp.generator." + type.makeParserName() + "Generator";
			var generatorClass = Class.forName(generatorName);
			var generator = (IGenerator) generatorClass.getDeclaredConstructor().newInstance();
			generator.initialize(cm);
			generatorMap.put(type, generator);
			logger.info("added generator: " + generatorName);
		}

		final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		for (var schedule : scheduleList) {
			if (!schedule.isPractice()) {
				continue;
			}

			var date = schedule.date();
			var exerciseYear = String.valueOf(date.getYear());

			var referencePath = Path.of(generationPathString, exerciseYear, date.toString());
			FileUtils.createDirectory(referencePath);

			var instructionPath = Path.of(instructionsPath.toString(), exerciseYear, date.toString());
			FileUtils.createDirectory(instructionPath);

			var newInstructionPath = Path.of(newInstructionsPath.toString(), exerciseYear, date.toString());

			if (legacyDate != null && date.isAfter(legacyDate)) {
				// after legacy date: generate!
				var messageType = schedule.messageType();
				var generator = generatorMap.get(messageType);
				var m = generator.generateMessage(date, schedule);
				var instructions = generator.generateIntructions(m, date, schedule, enableFinalize, now);

				var objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
				var json = objectMapper.writeValueAsString(m);
				Files.writeString(Path.of(referencePath.toString(), DTF.format(date) + "-reference.json"), json);

				var markerString = messageType.toString() + "-" + ((enableFinalize) ? "published" : "generated") + "-"
						+ timestampString + ".txt";
				Files.writeString(Path.of(referencePath.toString(), markerString), commitMessage);

				var ord = PracticeUtils.getOrdinalDayOfWeek(date);
				var ordName = PracticeUtils.getOrdinalLabel(ord);
				logger.info("generated date: " + date + ", " + ordName + " " + date.getDayOfWeek().toString() + ", "
						+ messageType.name());

				Files.writeString(Path.of(instructionPath.toString(), DTF.format(date) + "-instructions.txt"),
						instructions);

				FileUtils.createDirectory(newInstructionPath);
				Files.writeString(Path.of(newInstructionPath.toString(), DTF.format(date) + "-instructions.txt"),
						instructions);
			} else {
				// copy old reference.json, plus marker file from (old) reference
				// copy old instructions in reference, to instructions/
				// DO NOT COPY OLD instructions to new instructions

				var oldPath = Path.of(oldReferencePathString, exerciseYear, date.toString());
				var oldDir = oldPath.toFile();
				if (!oldDir.exists()) {
					logger.warn("reference dir: " + oldPath.toString() + " doesn't exist, but before legacyDate: "
							+ legacyDate.toString() + ", skipping");
					continue;
				}
				try (Stream<Path> stream = Files.list(oldPath)) {
					var paths = stream.filter(Files::isRegularFile) // Filters out subdirectories
							.toList();

					for (var path : paths) {
						var fileName = path.getFileName().toString();
						Path newPath = null;
						if (fileName.contains("instruction")) {
							newPath = Path.of(instructionPath.toString(), fileName);
						} else {
							newPath = Path.of(referencePath.toString(), fileName);
						}
						Files.copy(path, newPath);
					} // end loop over path in paths
				} // end try over stream
			} // end if before legacyDate
		} // end loop over schedules
	} // end function generateExercises

	private void doFinalization() throws Exception {
		logger.info("### BEGIN FINALIZATION");

		// copy old-reference publication-history folder
		var oldPubHistoryPath = Path.of(oldReferencePathString, "publication-history");
		var newPubHistoryPath = Path.of(generationPathString, "publication-history");
		FileUtils.copyDirectory(oldPubHistoryPath, newPubHistoryPath);
		logger.info("copied " + oldPubHistoryPath.toString() + " to " + newPubHistoryPath);

		// write new publication record
		var markerString = "published-" + timestampString + ".txt";
		Files.writeString(Path.of(newPubHistoryPath.toString(), markerString), commitMessage);
		logger.info("wrote publication record: " + markerString + " to " + newPubHistoryPath.toString());

		// copy publication history to instructions/ and new-instructions/
		FileUtils.copyDirectory(newPubHistoryPath, Path.of(instructionsPath.toString(), "publication-history"));
		FileUtils.copyDirectory(newPubHistoryPath, Path.of(newInstructionsPath.toString(), "publication-history"));
		logger.info("copied publication-history to instructions/ and new-instructions/");

		// copy schedule.csv to resources
		var oldSchedulePathString = cm.getAsString(GenKey.PATH_SCHEDULE);
		Files.copy(Path.of(generationPathString, "schedule.csv"), Path.of(oldSchedulePathString),
				StandardCopyOption.REPLACE_EXISTING);
		logger.info("copied schedule.csv to " + oldSchedulePathString);

		// copy new-instructions to all REMOTE publication sinks

		// copy generation to all REMOTE archive sinks

		// notify folks via email
		var body = "Date: " + now.toLocalDate().toString() //
				+ ", Time: " + now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "\n" //
				+ commitMessage;

		EmailService.sendSimpleEmail(cm, "New ETO Practice Instructions published!", body);

		// delete old-reference
		// copy generationPath to reference

		logger.info("### END FINALIZATION");
	} // end function doFinalization

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
					var name = ExcelUtils.getStringValue(row, 0);

					var extraData = ExcelUtils.getStringValue(row, 6);

					var ordinalString = ExcelUtils.getStringValue(row, 1);
					int ordinal = -1;
					try {
						var d = Double.parseDouble(ordinalString);
						ordinal = (int) d;
					} catch (Exception e) {
						logger.error(cnp + "ordinal: " + ordinalString + ", " + e.getMessage());
						System.exit(1);
					}

					var dowString = ExcelUtils.getStringValue(row, 2);
					var dow = DayOfWeek.valueOf(dowString.toUpperCase());
					if (dow == null) {
						logger.error(cnp + "Day of Week: " + dowString);
						System.exit(1);
					}

					var month = (Month) null;
					var monthString = ExcelUtils.getStringValue(row, 3);
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

					var yearString = ExcelUtils.getStringValue(row, 4);
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

					var messageTypesString = ExcelUtils.getStringValue(row, 5);
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
					var internalRecord = new InternalRecord(name, ordinal, dow, month, year, chooser, isPractice,
							extraData);
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
						logger.debug(
								"skipping IR: " + input.name + ", date: " + date.toString() + ", ordinal mismatch");
						continue;
					}

					if (!dayOfWeek.equals(input.dayOfWeek)) {
						logger.debug(
								"skipping IR: " + input.name + ", date: " + date.toString() + ", day of week mismatch");
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
