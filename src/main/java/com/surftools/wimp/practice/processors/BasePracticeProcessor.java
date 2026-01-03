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
import java.time.DayOfWeek;
import java.time.LocalDateTime;
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

import com.surftools.utils.MultiDateTimeParser;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.counter.ICounter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
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
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.processors.std.AcknowledgementProcessor;
import com.surftools.wimp.processors.std.AcknowledgementProcessor.AckEntry;
import com.surftools.wimp.processors.std.AcknowledgementProcessor.AckKey;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.service.chart.ChartServiceFactory;
import com.surftools.wimp.service.map.MapContext;
import com.surftools.wimp.service.map.MapEntry;
import com.surftools.wimp.service.map.MapLayer;
import com.surftools.wimp.service.map.MapService;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;
import com.surftools.wimp.service.simpleTestService.TestResult;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * place as much common processing here
 */
public abstract class BasePracticeProcessor extends AbstractBaseProcessor {
  protected Logger logger = LoggerFactory.getLogger(BasePracticeProcessor.class);

  protected Set<MessageType> messageTypesRequiringSecondaryAddress = new HashSet<>();
  protected Set<String> secondaryDestinations = new LinkedHashSet<>();

  protected String sender;

  protected LocalDateTime windowOpenDT = null;
  protected LocalDateTime windowCloseDT = null;

  protected SimpleTestService sts = new SimpleTestService();

  protected LatLongPair feedbackLocation = null;
  protected Map<String, IWritableTable> mIdFeedbackMap = new HashMap<String, IWritableTable>();
  protected List<String> badLocationMessageIds = new ArrayList<String>();

  protected String outboundMessagePrefixContent = "";
  protected String outboundMessagePostfixContent = "";
  protected String outboundMessageExtraContent = "";

  protected MessageType exerciseMessageType; // the messageType for the date/exercise
  protected MessageType processorMessageType; // the messageType associated with a Processor
  protected ExportedMessage referenceMessage;

  protected static List<OutboundMessage> outboundMessageList;
  protected static String outboundMessageSender;
  protected static String outboundMessageSubject;
  protected static boolean doOutboundMessaging;

  protected List<PracticeSummary> practiceSummaries = new ArrayList<>();

  protected Map<String, AckEntry> ackMap;

  protected String nextInstructions;

  protected final List<String> clearinghouseList = new ArrayList<String>();

  protected ExportedMessage message;

  protected int ppCount = 0;
  protected int ppMessageCount = 0;
  protected int ppMessageCorrectCount = 0;

  public Map<String, Counter> counterMap = new LinkedHashMap<String, Counter>();

  @SuppressWarnings("unchecked")
  public void initialize(IConfigurationManager cm, IMessageManager mm, MessageType _processorMessageType) {

    processorMessageType = _processorMessageType;

    // only initialize once; not five times
    var exerciseMessageTypeString = cm.getAsString(Key.EXPECTED_MESSAGE_TYPES);
    exerciseMessageType = MessageType.fromString(exerciseMessageTypeString);
    if (exerciseMessageType != processorMessageType) {
      logger.debug("processor messageType (" + processorMessageType.name() + ") != exercise messageType("
          + exerciseMessageType.name() + "), skipping");
      return;
    }

    super.initialize(cm, mm);

    if (!PracticeGeneratorTool.VALID_MESSAGE_TYPES.contains(exerciseMessageType)) {
      throw new IllegalArgumentException("unsupported messageType: " + exerciseMessageTypeString);
    }

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

    referenceMessage = (ExportedMessage) mm.getContextObject(PracticeProcessorTool.REFERENCE_MESSAGE_KEY);

    ackMap = (Map<String, AckEntry>) mm.getContextObject(AcknowledgementProcessor.ACK_MAP);

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
      var extraContentPathName = cm.getAsString(Key.PATH_NAG_CONTENT);
      if (extraContentPathName != null) {
        try {
          var extraContentPath = Path.of(extraContentPathName);
          var lines = Files.readAllLines(extraContentPath);
          var extraContent = String.join("\n", lines.stream().filter(s -> !s.trim().startsWith("#")).toList()).trim();
          if (extraContent != null && extraContent.length() > 0) {
            outboundMessageExtraContent = extraContent;
            logger.info("file: " + extraContentPathName + " provides the following extra content:\n"
                + outboundMessageExtraContent);
          }
        } catch (Exception e) {
          logger.warn("Could not get extra content for outbound messages. Using default");
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
    count(sts.test("To and Cc list should not contain monthly/training/clearinghouse addresses", pred, intersection));
  }

  protected void endCommonProcessing(ExportedMessage m) {
    var ackEntry = ackMap.get(m.from);
    var ackKey = new AckKey(m.from, m.messageId, m.getMessageType());
    var unexpectedMessage = ackEntry.unexpectedMessageMap.get(ackKey);
    if (unexpectedMessage != null) {
      ackEntry.unexpectedMessageMap.remove(ackKey);
      ackEntry.expectedMessageMap.put(ackKey, m);
    }
    var ackText = AcknowledgementProcessor.makeText(m.from);

    if (feedbackLocation == null || feedbackLocation.equals(LatLongPair.ZERO_ZERO)) {
      feedbackLocation = LatLongPair.ZERO_ZERO;
      badLocationMessageIds.add(m.messageId);
      sts.test("LAT/LON should be provided", false, "missing");
    } else if (!feedbackLocation.isValid()) {
      sts.test("LAT/LON should be provided", false, "invalid " + feedbackLocation.toString());
      feedbackLocation = LatLongPair.ZERO_ZERO;
      badLocationMessageIds.add(m.messageId);
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
    mIdFeedbackMap.put(m.messageId, new FeedbackMessage(feedbackResult, m));

    // var ackText = ackTextMap.get(sender);
    var sb = new StringBuilder();
    sb.append("ACKNOWLEDGEMENTS" + "\n");
    sb.append(ackText);
    sb.append("FEEDBACK" + "\n");

    if (doOutboundMessaging) {
      outboundMessagePrefixContent = sb.toString();
      outboundMessagePostfixContent = nextInstructions;

      var outboundMessageFeedback = outboundMessagePrefixContent + feedback + outboundMessagePostfixContent;
      var outboundMessage = new OutboundMessage(outboundMessageSender, sender, //
          makeOutboundMessageSubject(m), outboundMessageFeedback, null);
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
      logger.info("adjusting lat/long for " + badLocationMessageIds.size() + " messages: "
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

    // // this is how we get feedback to folks who only send unexpected messages,
    // // back-port?
    var enableFeedbackForOnlyUnexpected = true;
    if (enableFeedbackForOnlyUnexpected) {
      // var ackTextMap = (Map<String, String>)
      // (mm.getContextObject(AcknowledgementProcessor.ACK_TEXT_MAP));
      var nextInstructions = (String) mm.getContextObject(PracticeProcessorTool.INSTRUCTIONS_KEY);
      var allSenderSet = new HashSet<String>(ackMap.keySet());
      var expectedSenderList = outboundMessageList.stream().map(m -> m.to()).toList();
      allSenderSet.removeAll(expectedSenderList);
      var unexpectedSenderSet = allSenderSet;
      logger.info("Senders who only sent unexpected messages: " + String.join(",", unexpectedSenderSet));
      for (var sender : unexpectedSenderSet) {
        var ackText = AcknowledgementProcessor.makeText(sender);
        var sb1 = new StringBuilder();
        sb1.append("ACKNOWLEDGEMENTS" + "\n");
        sb1.append(ackText);
        sb1.append("FEEDBACK" + "\n");
        sb1.append("no " + exerciseMessageType.name() + " message received");
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

    var chartService = ChartServiceFactory.getChartService(cm);
    chartService.initialize(cm, counterMap, exerciseMessageType);
    chartService.makeCharts();

    var dateString = cm.getAsString(Key.EXERCISE_DATE);
    var mapService = new MapService(cm, mm);

    // feedback map
    var feedbackCounter = new Counter();
    final int nLayers = 6;
    var truncatedCountMap = new HashMap<Integer, Integer>(); // 9 -> 9 or more
    for (var summary : mIdFeedbackMap.values()) {
      var feedbackMessage = (FeedbackMessage) summary;
      var count = feedbackMessage.feedbackResult().feedbackCount();
      var key = Math.min(nLayers - 1, count);
      var value = truncatedCountMap.getOrDefault(key, Integer.valueOf(0));
      ++value;
      truncatedCountMap.put(key, value);
    }

    var gradientMap = mapService.makeGradientMap(120, 0, nLayers);
    var layers = new ArrayList<MapLayer>();
    var countLayerNameMap = new HashMap<Integer, String>();
    for (var i = 0; i < nLayers; ++i) {
      var value = String.valueOf(i);
      var count = truncatedCountMap.getOrDefault(i, Integer.valueOf(0));
      if (i == nLayers - 1) {
        value = i + " or more";
      }
      var layerName = "value: " + value + ", count: " + count;
      countLayerNameMap.put(i, layerName);

      var color = gradientMap.get(i);
      var layer = new MapLayer(layerName, color);
      layers.add(layer);
    }

    var mapEntries = new ArrayList<MapEntry>(mIdFeedbackMap.values().size());
    for (var s : mIdFeedbackMap.values()) {
      var feedbackMessage = (FeedbackMessage) s;
      var m = feedbackMessage.message();
      var r = feedbackMessage.feedbackResult();
      var count = r.feedbackCount();

      final var lastColorMapIndex = gradientMap.size() - 1;
      final var lastColor = gradientMap.get(lastColorMapIndex);

      var location = m.mapLocation;
      var color = gradientMap.getOrDefault(count, lastColor);
      var prefix = "<b>" + m.from + "</b><hr>";
      var content = prefix + "Feedback Count: " + r.feedbackCount() + "\n" + "Feedback: " + r.feedback();
      var mapEntry = new MapEntry(m.from, m.to, location, content, color);
      mapEntries.add(mapEntry);
      feedbackCounter.increment(r.feedbackCount());
    }

    var legendTitle = dateString + " Feedback Counts (" + mIdFeedbackMap.values().size() + " total)";
    var context = new MapContext(publishedPath, //
        dateString + "-map-feedbackCount", // file name
        dateString + " Feedback Counts", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);

    WriteProcessor.writeTable(new ArrayList<IWritableTable>(practiceSummaries), "practice-summary.csv");

    var db = new PersistenceManager(cm);
    var input = makeDbInput(practiceSummaries);
    var dbResult = db.bulkInsert(input);
    if (dbResult.status() == ReturnStatus.ERROR) {
      logger.error("### database update failed: " + dbResult.content());
    }
  }

  protected LocalDateTime parseDateTime(String dateString, String timeString) {
    final var list = List.of(//
        "yyyy-MM-dd HH:mm", // default
        "yyyy-MM-dd HH:mm 'Z'", // default, with Z
        "yyyy/MM/dd HH:mm" // default with slashes
    );
    final var mdtp = new MultiDateTimeParser(list);
    return mdtp.parseDateTime(dateString + " " + timeString);
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
}
