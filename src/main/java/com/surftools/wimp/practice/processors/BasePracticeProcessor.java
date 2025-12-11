/**

The MIT License (MIT)

Copyright (c) 2024, Robert Tykulsker

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

import static java.time.temporal.ChronoUnit.DAYS;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.counter.ICounter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IProcessor;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.feedback.FeedbackMessage;
import com.surftools.wimp.feedback.FeedbackResult;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.message.Hics259Message;
import com.surftools.wimp.message.Ics205Message;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.persistence.PersistenceManager;
import com.surftools.wimp.persistence.dto.BulkInsertEntry;
import com.surftools.wimp.persistence.dto.Event;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.ReturnStatus;
import com.surftools.wimp.practice.generator.PracticeUtils;
import com.surftools.wimp.practice.misc.PracticeSummary;
import com.surftools.wimp.practice.tools.PracticeGeneratorTool;
import com.surftools.wimp.practice.tools.PracticeProcessorTool;
import com.surftools.wimp.processors.std.AcknowledgementProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.service.chart.ChartServiceFactory;
import com.surftools.wimp.service.map.MapEntry;
import com.surftools.wimp.service.map.MapHeader;
import com.surftools.wimp.service.map.MapService;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;
import com.surftools.wimp.service.simpleTestService.TestResult;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * place as much common processing here
 */
public abstract class BasePracticeProcessor implements IProcessor {
  protected Logger logger = LoggerFactory.getLogger(BasePracticeProcessor.class);
  protected IConfigurationManager cm;
  protected IMessageManager mm;

  // fail-fast stuff first

  // we will be called by each PracticeProcessor, we only want and need to initialize once
  static boolean isInitialized;
  protected Set<MessageType> messageTypesRequiringSecondaryAddress = new HashSet<>();
  protected Set<String> secondaryDestinations = new LinkedHashSet<>();

  protected String dateString;
  protected LocalDate date;

  protected String sender;

  protected LocalDateTime windowOpenDT = null;
  protected LocalDateTime windowCloseDT = null;

  protected SimpleTestService sts = new SimpleTestService();

  protected Map<String, Counter> summaryCounterMap = new LinkedHashMap<String, Counter>();

  protected int ppCount = 0;
  protected int ppMessageCorrectCount = 0;

  protected LatLongPair feedbackLocation = null;
  protected Map<String, IWritableTable> mIdFeedbackMap = new HashMap<String, IWritableTable>();
  protected List<String> badLocationMessageIds = new ArrayList<String>();

  protected String outboundMessagePrefixContent = "";
  protected String outboundMessagePostfixContent = "";
  protected String outboundMessageExtraContent = OB_DISCLAIMER;

  protected MessageType exerciseMessageType; // the messageType for the date/exercise
  protected MessageType processorMessageType; // the messageType associated with a Processor

  protected static String pathName;
  protected static String outputPathName;
  protected static Path outputPath;

  protected static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  protected static final DateTimeFormatter ALT_DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING.replaceAll("-", "/"));
  protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  protected ExportedMessage message;

  public int ppMessageCount = 0;
  public int ppParticipantCount = 0;
  public int ppParticipantCorrectCount = 0;

  public Map<String, Counter> counterMap = new LinkedHashMap<String, Counter>();

  protected static List<OutboundMessage> outboundMessageList;
  protected static String outboundMessageSender;
  protected static String outboundMessageSubject;
  protected static boolean doOutboundMessaging;

  protected ExportedMessage referenceMessage;

  protected List<PracticeSummary> practiceSummaries = new ArrayList<>();

  protected Map<String, String> ackTextMap;

  protected String nextInstructions;

  protected final List<String> clearinghouseList = new ArrayList<String>();

  public void initialize(IConfigurationManager cm, IMessageManager mm, Logger _logger) {
    if (!isInitialized) {
      doInitialize(cm, mm, logger);
      isInitialized = true;
    }
  }

  @SuppressWarnings("unchecked")
  private void doInitialize(IConfigurationManager cm, IMessageManager mm, Logger _logger) {
    this.cm = cm;
    this.mm = mm;
    logger = _logger;

    // fail-fast stuff first;
    var messageTypeString = cm.getAsString(Key.EXPECTED_MESSAGE_TYPES);
    exerciseMessageType = MessageType.fromString(messageTypeString);
    if (exerciseMessageType == null) {
      throw new IllegalArgumentException("unknown messageType: " + messageTypeString);
    }

    if (!PracticeGeneratorTool.VALID_MESSAGE_TYPES.contains(exerciseMessageType)) {
      throw new IllegalArgumentException("unsupported messageType: " + messageTypeString);
    }

    dateString = cm.getAsString(Key.EXERCISE_DATE);
    date = LocalDate.parse(dateString);

    var ord = PracticeUtils.getOrdinalDayOfWeek(date);
    var dow = date.getDayOfWeek();
    if (dow != DayOfWeek.THURSDAY) {
      throw new RuntimeException("Exercise Date: " + dateString + " is NOT a THURSDAY, but is " + dow.toString());
    }

    var ordinalList = new ArrayList<Integer>(PracticeGeneratorTool.VALID_ORDINALS);
    Collections.sort(ordinalList);
    var ordinalLabels = ordinalList.stream().map(i -> PracticeUtils.getOrdinalLabel(i)).toList();
    if (!PracticeGeneratorTool.VALID_ORDINALS.contains(ord)) {
      throw new RuntimeException(
          "Exercise Date: " + dateString + " is NOT one of " + String.join(",", ordinalLabels) + " THURSDAYS");
    }

    windowOpenDT = LocalDateTime.from(DTF.parse(cm.getAsString(Key.EXERCISE_WINDOW_OPEN)));
    windowCloseDT = LocalDateTime.from(DTF.parse(cm.getAsString(Key.EXERCISE_WINDOW_CLOSE)));

    pathName = cm.getAsString(Key.PATH);
    // fail fast: our working directory, where our input files are
    Path path = Paths.get(pathName);
    if (!Files.exists(path)) {
      throw new IllegalArgumentException("specified path: " + pathName + " does not exist");
    }
    logger.info("Starting with input path: " + path);

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

    referenceMessage = (ExportedMessage) mm.getContextObject(PracticeProcessorTool.REFERENCE_MESSAGE_KEY);

    ackTextMap = (Map<String, String>) mm.getContextObject(AcknowledgementProcessor.ACK_TEXT_MAP);

    nextInstructions = (String) mm.getContextObject(PracticeProcessorTool.INSTRUCTIONS_KEY);

    for (var i = 1; i <= 9; ++i) {
      clearinghouseList.add("ETO-0" + i + "@winlink.org");
    }
    for (var extra : List.of("ETO-10", "ETO-BK", "ETO-CAN", "ETO-DX")) {
      clearinghouseList.add(extra + "@winlink.org");
    }

    outboundMessageSender = cm.getAsString(Key.OUTBOUND_MESSAGE_SENDER);
    outboundMessageSubject = cm.getAsString(Key.OUTBOUND_MESSAGE_SUBJECT);
    doOutboundMessaging = outboundMessageSender != null && outboundMessageSubject != null
        && !outboundMessageSender.isEmpty() && !outboundMessageSubject.isEmpty();
    outboundMessageList = new ArrayList<>();
    if (doOutboundMessaging) {
      outboundMessageExtraContent = "";
      var extraContentPathName = cm.getAsString(Key.PRACTICE_PATH_NAG_CONTENT);
      if (extraContentPathName != null) {
        try {
          var extraContentPath = Path.of(extraContentPathName);
          var lines = Files.readAllLines(extraContentPath);
          var extraContent = String.join("\n", lines.stream().filter(s -> !s.trim().startsWith("#")).toList()).trim();
          if (extraContent != null && extraContent.length() > 0) {
            outboundMessageExtraContent = extraContent;
            logger
                .info("file: " + extraContentPathName + " provides the following extra content:\n"
                    + outboundMessageExtraContent);
          }
        } catch (Exception e) {
          logger.error("Could not get extra content for outbound messages. Using default");
        }
      }
    }
  }

  @Override
  public void process() {
    if (exerciseMessageType != processorMessageType) {
      logger.info("skipping processing for messageType: " + processorMessageType.name());
      return;
    }

    var senderIterator = mm.getSenderIterator();
    while (senderIterator.hasNext()) { // loop over senders
      sender = senderIterator.next();

      // process all messages for a type, in ascending chronological order
      var messages = mm.getMessagesForSender(sender).get(exerciseMessageType);
      if (messages == null || messages.size() == 0) {
        continue;
      }
      for (var message : messages) {
        beginCommonProcessing(message);
        specificProcessing(message);
        endCommonProcessing(message);
      } // end processing for a message
    }
  }

  protected void beginCommonProcessing(ExportedMessage message) {
    ++ppCount;
    sts.reset(sender);

    feedbackLocation = message.mapLocation;

    if (secondaryDestinations.size() > 0) {
      if (messageTypesRequiringSecondaryAddress.size() == 0
          || messageTypesRequiringSecondaryAddress.contains(message.getMessageType())) {
        var addressList = (message.toList + "," + message.ccList).toUpperCase();
        for (var ev : secondaryDestinations) {
          count(sts.test("To and/or CC addresses should contain " + ev, addressList.contains(ev)));
        }
      }
    }

    if (windowOpenDT != null && windowCloseDT != null) {
      sts.testOnOrAfter("Message should be posted on or after #EV", windowOpenDT, message.msgDateTime, DTF);
      sts.testOnOrBefore("Message should be posted on or before #EV", windowCloseDT, message.msgDateTime, DTF);

      var daysAfterOpen = DAYS.between(windowOpenDT, message.msgDateTime);
      getCounter("Message sent days after window opens").increment(daysAfterOpen);
    }

    var addressesString = message.toList + "," + message.ccList;
    var addressesList = Arrays.asList(addressesString.split(","));

    count(sts.testList("Addresses should contain ETO-PRACTICE@winlink.org", addressesList, "ETO-PRACTICE@winlink.org"));

    var result = addressesList.stream().distinct().filter(clearinghouseList::contains).toList();
    var intersection = String.join(",", result);
    var pred = intersection.length() == 0;
    count(
        sts.test("To and Cc list should not contain \"monthly/training/clearinghouse\" addresses", pred, intersection));
  }

  protected void endCommonProcessing(ExportedMessage message) {

    if (feedbackLocation == null || feedbackLocation.equals(LatLongPair.ZERO_ZERO)) {
      feedbackLocation = LatLongPair.ZERO_ZERO;
      badLocationMessageIds.add(message.messageId);
      sts.test("LAT/LON should be provided", false, "missing");
    } else if (!feedbackLocation.isValid()) {
      sts.test("LAT/LON should be provided", false, "invalid " + feedbackLocation.toString());
      feedbackLocation = LatLongPair.ZERO_ZERO;
      badLocationMessageIds.add(message.messageId);
    } else {
      sts.test("LAT/LON should be provided", true, null);
    }

    var explanations = sts.getExplanations();
    var feedback = "";
    getCounter("Feedback Count").increment(explanations.size());
    if (explanations.size() == 0) {
      ++ppMessageCorrectCount;
      feedback = "Perfect Message!";
    } else {
      feedback = String.join("\n", explanations);
    }

    var feedbackResult = new FeedbackResult(sender, feedbackLocation.getLatitude(), feedbackLocation.getLongitude(),
        explanations.size(), feedback);
    mIdFeedbackMap.put(message.messageId, new FeedbackMessage(feedbackResult, message));

    var ackText = ackTextMap.get(sender);
    var sb = new StringBuilder();
    sb.append("ACKNOWLEDGEMENTS" + "\n");
    sb.append(ackText);
    sb.append("FEEDBACK" + "\n");

    if (doOutboundMessaging) {
      outboundMessagePrefixContent = sb.toString();
      outboundMessagePostfixContent = nextInstructions;

      var outboundMessageFeedback = outboundMessagePrefixContent + feedback + outboundMessagePostfixContent;
      var outboundMessage = new OutboundMessage(outboundMessageSender, sender, //
          makeOutboundMessageSubject(message), outboundMessageFeedback, null);
      outboundMessageList.add(outboundMessage);
    }
  }

  /**
   * get Counter for label, create if needed
   *
   * @param label
   * @return
   */
  protected Counter getCounter(String label) {
    var counter = counterMap.get(label);
    if (counter == null) {
      counter = new Counter(label);
      counterMap.put(label, counter);
    }

    return counter;
  }

  /**
   * get a TestResult!
   *
   * @param testResult
   */
  protected void count(TestResult testResult) {
    var label = testResult.key();
    var result = testResult.ok();
    getCounter(label).increment(result ? "correct" : "incorrect");
  }

  protected String makeOutboundMessageSubject(Object object) {
    var message = (ExportedMessage) object;
    return outboundMessageSubject + " " + message.messageId;
  }

  /**
   * this is the exercise-specific method to be implemented
   *
   * @param message
   */
  protected void specificProcessing(ExportedMessage message) {
  }

  @Override
  public void postProcess() {
    if (exerciseMessageType != processorMessageType) {
      logger.info("skipping post-processing for messageType: " + processorMessageType.name());
      return;
    }

    var sb = new StringBuilder();
    sb.append("\n\n" + "aggregate results for " + exerciseMessageType.toString() + " on " + dateString + ":\n");
    sb.append("Participants: " + ppCount + "\n");
    sb.append(formatPP("Correct Messages", ppMessageCorrectCount, ppCount));

    var it = sts.iterator();
    while (it.hasNext()) {
      var key = it.next();
      if (sts.hasContent(key)) {
        sb.append(sts.format(key));
      }
    }

    sb.append("\n-------------------Histograms---------------------\n");
    for (var counterLabel : counterMap.keySet()) {
      sb.append(formatCounter(counterLabel, counterMap.get(counterLabel)));
    }

    logger.info(sb.toString());

    if (badLocationMessageIds.size() > 0) {
      logger
          .info("adjusting lat/long for " + badLocationMessageIds.size() + " messages: "
              + String.join(",", badLocationMessageIds));
      var newLocations = LocationUtils.jitter(badLocationMessageIds.size(), LatLongPair.ZERO_ZERO, 10_000);
      for (int i = 0; i < badLocationMessageIds.size(); ++i) {
        var messageId = badLocationMessageIds.get(i);
        var feedbackMessage = (FeedbackMessage) mIdFeedbackMap.get(messageId);
        var newLocation = newLocations.get(i);
        feedbackMessage.message().mapLocation = newLocation;
        var newFeedbackMessage = feedbackMessage.updateLocation(newLocation);
        mIdFeedbackMap.put(messageId, newFeedbackMessage);
      }
    }

    // this is how we get feedback to folks who only send unexpected messages, back-port?
    var enableFeedbackForOnlyUnexpected = true;
    if (enableFeedbackForOnlyUnexpected) {
      final var DASHES = "----------------------------------------------------------------------------------------------";
      @SuppressWarnings("unchecked")
      var ackTextMap = (Map<String, String>) (mm.getContextObject(AcknowledgementProcessor.ACK_TEXT_MAP));
      var nextInstructions = (String) mm.getContextObject(PracticeProcessorTool.INSTRUCTIONS_KEY);
      var allSenderSet = new HashSet<String>(ackTextMap.keySet());
      var expectedSenderList = outboundMessageList.stream().map(m -> m.to()).toList();
      allSenderSet.removeAll(expectedSenderList);
      var unexpectedSenderSet = allSenderSet;
      logger.info("Senders who only sent unexpected messages: " + String.join(",", unexpectedSenderSet));
      for (var sender : unexpectedSenderSet) {
        var ackText = ackTextMap.get(sender);
        var sb1 = new StringBuilder();
        sb1.append("ACKNOWLEDGEMENTS" + "\n");
        sb1.append(ackText);
        sb1.append("\n" + DASHES + "\n");
        sb1.append("\nFEEDBACK" + "\n");
        sb1.append("no " + exerciseMessageType.name() + " message received\n");
        sb1.append("\n" + DASHES);
        sb1.append(nextInstructions);
        sb1.append(outboundMessagePostfixContent);
        var text = sb1.toString();
        var outboundMessage = new OutboundMessage(outboundMessageSender, sender, outboundMessageSubject, text, null);
        outboundMessageList.add(outboundMessage);
      }
    }

    var results = new ArrayList<>(mIdFeedbackMap.values());
    WriteProcessor.writeTable(results, "feedback-" + exerciseMessageType.toString() + ".csv");

    if (doOutboundMessaging) {
      var service = new OutboundMessageService(cm, mm, outboundMessageExtraContent, "allFeedback.txt");
      outboundMessageList = service.sendAll(outboundMessageList);
      WriteProcessor.writeTable(new ArrayList<IWritableTable>(outboundMessageList), "outBoundMessages.csv");
    }

    for (var key : counterMap.keySet()) {
      var summaryKey = exerciseMessageType.name() + "_" + key;
      var value = counterMap.get(key);
      summaryCounterMap.put(summaryKey, value);
    }

    var chartService = ChartServiceFactory.getChartService(cm);
    chartService.initialize(cm, counterMap, exerciseMessageType);
    chartService.makeCharts();

    var mapEntries = mIdFeedbackMap.values().stream().map(s -> MapEntry.fromSingleMessageFeedback(s)).toList();
    var mapService = new MapService(null, null);
    mapService.makeMap(outputPath, new MapHeader(cm.getAsString(Key.EXERCISE_NAME), ""), mapEntries);

    WriteProcessor.writeTable(new ArrayList<IWritableTable>(practiceSummaries), "practice-summary.csv");

    var db = new PersistenceManager(cm);
    var input = makeDbInput(practiceSummaries);
    var dbResult = db.bulkInsert(input);
    if (dbResult.status() == ReturnStatus.ERROR) {
      logger.error("### database update failed: " + dbResult.content());
    }
  }

  protected String formatPercent(Double d) {
    return d == null ? "" : String.format("%.2f", 100d * d) + "%";
  }

  protected String formatPP(String label, int okCount, int ppCount) {
    var notOkCount = ppCount - okCount;
    var okPercent = okCount / (double) ppCount;
    var notOkPercent = 1d - okPercent;
    return "  " + label + ": " //
        + okCount + "(" + formatPercent(okPercent) + ") ok, " //
        + notOkCount + "(" + formatPercent(notOkPercent) + ") not ok" //
        + "\n";
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected String formatCounter(String label, ICounter counter) {
    var it = counter.getDescendingCountIterator();
    var sb = new StringBuilder();
    while (it.hasNext()) {
      var entry = (Entry<Comparable, Integer>) it.next();
      sb.append(" " + "value" + ": " + entry.getKey() + ", " + "count" + ": " + entry.getValue() + "\n");
    }

    return "\n" + label + ":\n" + sb.toString();
  }

  private BulkInsertEntry makeDbInput(List<PracticeSummary> summaries) {
    var exerciseId = getExerciseId(referenceMessage);
    Exercise exercise = new Exercise(-1, date, "Practice", exerciseId, exerciseMessageType.name());
    List<Event> events = new ArrayList<>();
    for (var summary : summaries) {
      var event = new Event(-1, -1, -1, summary.from, summary.location, //
          summary.feedbackCount, summary.getFeedback(), //
          "{\"messageId\":\"" + summary.messageId + "\"}");
      events.add(event);
    }
    return new BulkInsertEntry(exercise, events);
  }

  private String getExerciseId(ExportedMessage ref) {
    String fullExerciseId = null;
    var messageType = ref.getMessageType();
    switch (messageType) {
    case ICS_213:
      fullExerciseId = ((Ics213Message) ref).formMessage;
      break;
    case ICS_213_RR:
      fullExerciseId = ((Ics213RRMessage) ref).requestNumber;
      break;
    case HICS_259:
      fullExerciseId = ((Hics259Message) ref).incidentName;
      break;
    case ICS_205:
      fullExerciseId = ((Ics205Message) ref).specialInstructions;
      break;
    case FIELD_SITUATION:
      fullExerciseId = ((FieldSituationMessage) ref).additionalComments;
      break;
    default:
      throw new RuntimeException("unsupported messageType: " + ref.getMessageType().toString());
    }

    if (fullExerciseId == null) {
      return null;
    }

    var fields = fullExerciseId.split(" ");
    if (fields.length == 3) {
      return fields[2].trim();
    } else {
      return null;
    }
  }

  public static final String OB_DISCLAIMER = """
      -----------------------------------------------------------------------------------------------------

      DISCLAIMER: This feedback is provided for your consideration. We use the results to improve future
      exercises. Differences in spelling, numbers or omitting whitespace  will trigger this automated message.
      Differences in capitalization, punctuation and extra whitespace are generally ignored. You may
      think that some of our feedback is "nit picking" and that your responses would be understood by any
      reasonable person -- and you'd be correct! You're welcome to disagree with any or all of our feedback.
      You're also welcome to reply via Winlink to this message or send an email to
      ETO.Technical.Team@emcomm-training.groups.io. In any event, thank you for participating
      in this exercise. We look forward to seeing you at our next Winlink Thursday Exercise!
      """;

  public String getNagString(int year, int textId) {
    final String text_2023 = """

        -----------------------------------------------------------------------------------------------------

          ETO needs sponsors to be able to renew our groups.io subscription for $YEAR.
          By sponsoring this group, you are helping pay the Groups.io hosting fees.
          Here is the link to sponsor our group:  https://emcomm-training.groups.io/g/main/sponsor
          Any amount you sponsor will be held by Groups.io and used to pay hosting fees as needed.
          The minimum sponsorship is $5.00.

          Thank you for your support!

          """;

    final String text_2024 = """

        -----------------------------------------------------------------------------------------------------

           Please consider supporting ETO operations by making a cash donation and/or sponsoring
           our groups.io site. Our goal for cash donations is $500 which would be used to cover
           the hosting fees for our website, annual governmental fees, bank fees and a post office box.
           The goal for sponsorship of our groups.io site is $220 which covers the fees for 2025.

           You can make cash donations on our website by using this link:
               https://emcomm-training.org/Donate.html
           Note: Cash donations are tax-deductible as allowed by law.

           You can use this link to sponsor our group:
               https://emcomm-training.groups.io/g/main/sponsor
           Note:  Sponsorships are not tax deductible since the money is paid to groups.io.
           The minimum sponsorship is $5.00.

           If you want to donate using another payment method, please send an email to
           emcommtrainingorg@gmail.com.

           Thank you for your support !!!!!!

           EmComm Training Organization is a nonprofit, tax-exempt charitable organization
           (tax ID number 92-2282844) under Section 501(c)(3) of the Internal Revenue Code.
           Donations are tax-deductible as allowed by law.

           """;

    final var texts = new String[] { null, text_2023, text_2024 };
    var text = texts[textId];
    var result = text.replaceAll("\\$YEAR", String.valueOf(year));
    return result;
  }

  public static final String OB_REQUEST_FEEDBACK = """

      =====================================================================================================

      ETO would love to hear from you! Would you please take a few minutes to answer the following questions:
      1. Were the exercise instructions clear? If not, where did they need improvement?
      2. Did you find the exercise useful?
      3. Did you find the above feedback useful?
      4. What did you dislike about the exercise?
      5. Any additional comments?

      Please reply to this Winlink message or to ETO.Technical.Team@EmComm-Training.groups.io. Thank you!
      """;

}
