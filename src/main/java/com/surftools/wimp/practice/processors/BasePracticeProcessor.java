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
import java.util.function.Function;
import java.util.function.Predicate;

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
import com.surftools.wimp.message.PracticeMessage;
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
import com.surftools.wimp.service.map.IMapService;
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
  protected int relocationIndex;

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

    feedbackLocation = (message.getMessageType() == MessageType.FIELD_SITUATION) ? message.msgLocation
        : message.mapLocation;

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

    getCounter("Location source").increment(message.msgLocationSource);
    getCounter("Message Sender == Message Source").increment(message.source.equals(message.from));
  }

  protected void endCommonProcessing(ExportedMessage m) {
    var practiceMessage = new PracticeMessage(m);
    getCounter("Senders Form Version").increment(practiceMessage.getFormVersion());
    getCounter("Senders Express Version").increment(practiceMessage.getExpressVersion());

    var ackEntry = ackMap.get(m.from);
    var ackKey = new AckKey(m.from, m.messageId, m.getMessageType());
    var unexpectedMessage = ackEntry.unexpectedMessageMap.get(ackKey);
    if (unexpectedMessage != null) {
      ackEntry.unexpectedMessageMap.remove(ackKey);
      ackEntry.expectedMessageMap.put(ackKey, m);
    }
    var ackText = AcknowledgementProcessor.makeText(m.from);

    if (feedbackLocation == null || feedbackLocation.equals(LatLongPair.ZERO_ZERO)) {
      feedbackLocation = LocationUtils.binaryAngularSubdivision(relocationIndex++, null, 10_000d);
      sts.test("LAT/LON should be provided", false, "missing");
    } else if (!feedbackLocation.isValid()) {
      sts.test("LAT/LON should be provided", false, "invalid " + feedbackLocation.toString());
      feedbackLocation = LatLongPair.ZERO_ZERO;
      feedbackLocation = LocationUtils.binaryAngularSubdivision(relocationIndex++, null, 10_000d);
    } else {
      sts.test("LAT/LON should be provided", true, null);
    }

    var explanations = sts.getExplanations();
    var feedback = "";
    var count = explanations.size();
    var label = count >= 10 ? "10 or more" : String.valueOf(count);
    getCounter("Feedback Count").increment(label);
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

    var counterKey = "Senders Express Version";
    var counter = counterMap.get(counterKey);
    if (counter != null) {
      counter = counter.squeeze(10, "other");
      counterMap.put(counterKey, counter);
    }

    var chartService = ChartServiceFactory.getChartService(cm);
    chartService.initialize(cm, counterMap, exerciseMessageType);
    chartService.makeCharts();

    WriteProcessor.writeTable(new ArrayList<IWritableTable>(practiceSummaries), "practice-summary.csv");

    var mapEntries = makeMapEntries();

    var mapService = new MapService(cm, mm);
    var gradientMap = mapService.makeGradientMap(120, 0, 6);
    makeMakeViaLegends(mapEntries, "Feedback Counts", "FeedbackCount", publishedPath, List.of(//
        new Legend("value: 0", gradientMap.get(0), (me -> me.message().contains("Count: 0\n")), null), //
        new Legend("value: 1", gradientMap.get(1), (me -> me.message().contains("Count: 1\n")), null), //
        new Legend("value: 2", gradientMap.get(2), (me -> me.message().contains("Count: 2\n")), null), //
        new Legend("value: 3", gradientMap.get(3), (me -> me.message().contains("Count: 3\n")), null), //
        new Legend("value: 4", gradientMap.get(4), (me -> me.message().contains("Count: 4\n")), null), //
        new Legend("value: 5 or more", gradientMap.get(5), (me -> me != null), null)));

    var colorGood = IMapService.rgbMap.get("green");
    var colorBad = IMapService.rgbMap.get("red");

    makeMakeViaLegends(mapEntries, "Start Date Counts", "StartDate", outputPath, List.of(//
        new Legend("Correct", colorGood, (me -> !me.message().contains("Message should be posted on or after")), null), //
        new Legend("Incorrect", colorBad, (me -> me.message().contains("Message should be posted on or after")),
            null)));

    makeMakeViaLegends(mapEntries, "Exercise Id Counts", "ExerciseId", outputPath, List.of(//
        new Legend("Correct", colorGood, (me -> !me.message().contains("Exercise Id")), null), //
        new Legend("Incorrect", colorBad, (me -> me.message().contains("Exercise Id")), null)));

    makeMakeViaLegends(mapEntries, "Subject Counts", "Subject", outputPath, List.of(//
        new Legend("Correct", colorGood, (me -> !me.message().contains("Subject")), null), //
        new Legend("Incorrect", colorBad, (me -> me.message().contains("Subject")), null)));

    makeMessageTypeMap();

    var db = new PersistenceManager(cm);
    var input = makeDbInput(practiceSummaries);
    var dbResult = db.bulkInsert(input);
    if (dbResult.status() == ReturnStatus.ERROR) {
      logger.error("### database update failed: " + dbResult.content());
    }

    doLastWord(practiceSummaries);
  }

  private void doLastWord(List<PracticeSummary> practiceSummaries) {
    var originalMessages = mm.getOriginalMessages();
    var originalSenderSize = new HashSet<String>(originalMessages.stream().map(s -> s.from).toList()).size();
    var totalFeedbackCount = practiceSummaries.stream().mapToInt(PracticeSummary::getFeedbackCount).sum();
    var avgFeedbackCount = (double) totalFeedbackCount / (double) practiceSummaries.size();
    var exerciseDate = cm.getAsString(Key.EXERCISE_DATE);
    var lines = new ArrayList<String>();
    lines.add("Exercise date: " + exerciseDate);
    lines.add("Exercise message type: " + exerciseMessageType.toString());
    lines.add(" ");
    lines.add("Total messages received: " + originalMessages.size());
    lines.add("Total participants: " + originalSenderSize);
    lines.add("On-type participants: " + practiceSummaries.size());
    lines.add("Average feedback: " + String.format("%.02f", avgFeedbackCount));
    var lastWord = String.join("\n", lines);

    WriteProcessor.writeString(lastWord, Path.of(outputPathName, exerciseDate + "-lastWord.txt"));
    logger.info("adding lastWord: \n" + lastWord);
    mm.putContextObject(IMessageManager.LAST_WORD, lastWord);
  }

  record Legend(String label, String color, Predicate<MapEntry> predicate, Function<MapEntry, String> popupGenerator) {
  };

  private void makeMakeViaLegends(List<MapEntry> mapEntries, String legendTitle, String fileName, Path path,
      List<Legend> legends) {
    var colorCountMap = new HashMap<String, Integer>();

    var newMapEntries = new ArrayList<MapEntry>(mapEntries.size());
    for (var mapEntry : mapEntries) {
      var found = false;
      for (var legend : legends) {
        if (legend.predicate.test(mapEntry)) {
          var count = colorCountMap.getOrDefault(legend.color, Integer.valueOf(0));
          ++count;
          colorCountMap.put(legend.color, count);
          mapEntry = mapEntry.update(legend.color);
          newMapEntries.add(mapEntry);
          found = true;
          break;
        } // endif predicate matches
      } // end loop over legend entries
      if (!found) {
        logger.debug("not found");
      }
    } // end loop of mapEntries

    var layers = new ArrayList<MapLayer>();
    for (var legend : legends) {
      var color = legend.color;
      var label = legend.label;
      var count = colorCountMap.getOrDefault(color, Integer.valueOf(0));
      layers.add(new MapLayer(label + ", count: " + count, color));
    }

    legendTitle = dateString + " " + legendTitle + " (" + mIdFeedbackMap.values().size() + " total)";
    var context = new MapContext(path, //
        dateString + "-map-" + fileName, // file name
        dateString + legendTitle, // map title
        null, legendTitle, layers, newMapEntries);
    var mapService = new MapService(cm, mm);
    mapService.makeMap(context);
  }

  private List<MapEntry> makeMapEntries() {
    var mapEntries = new ArrayList<MapEntry>(mIdFeedbackMap.values().size());
    relocationIndex = 0;
    for (var s : mIdFeedbackMap.values()) {
      var feedbackMessage = (FeedbackMessage) s;
      var m = feedbackMessage.message();
      var r = feedbackMessage.feedbackResult();
      var location = (m.getMessageType() == MessageType.FIELD_SITUATION) ? m.msgLocation : m.mapLocation;
      if (location == null || !location.isValid()) {
        location = LocationUtils.binaryAngularSubdivision(relocationIndex++, LatLongPair.ZERO_ZERO, 10_000d);
      }
      var color = "";
      var prefix = "<b>" + m.from + "</b><hr>";
      var content = prefix + "Feedback Count: " + r.feedbackCount() + "\n" + "Feedback: " + r.feedback();
      content = content.replaceAll("\"", "&quot;");
      var mapEntry = new MapEntry(m.from, m.to, location, content, color);
      mapEntries.add(mapEntry);
    }

    return mapEntries;
  }

  private void makeMessageTypeMap() {
    var mapService = new MapService(cm, mm);
    var colorGreen = IMapService.rgbMap.get("green");
    var colorBlue = IMapService.rgbMap.get("blue");
    var colorRed = IMapService.rgbMap.get("red");

    // counts by participant/sender/call
    var expectedCount = 0; // expected only
    var mixedCount = 0; // both expected and unexpected messageTypes
    var unexpectedCount = 0; // unexpected only
    var mapEntries = new ArrayList<MapEntry>();
    var relocationIndex = 0;

    var it = mm.getSenderIterator();
    while (it.hasNext()) {
      var sender = it.next();

      var location = (LatLongPair) null;
      Map<MessageType, List<ExportedMessage>> map = mm.getMessagesForSender(sender);
      var senderMessageTypes = new ArrayList<MessageType>(map.keySet());
      Collections.sort(senderMessageTypes, ((t, o) -> t == exerciseMessageType ? 1 : t.name().compareTo(o.name())));
      var hasExpected = false;
      var hasUnexpected = false;
      var popupText = new StringBuilder("<b>" + sender + "</b><hr>" + "\n");
      for (var type : senderMessageTypes) {
        var messageList = map.get(type);
        var count = messageList == null ? "0" : String.valueOf(messageList.size());
        var mIds = messageList.stream().map(m -> m.messageId).toList();
        var mIdPrefix = mIds.size() == 1 ? "mId: " : "mIds: ";
        var mIdString = String.join(",", mIds);
        popupText.append("Type: " + type.name() + ", count: " + count + ", " + mIdPrefix + mIdString + "\n");
        if (type == exerciseMessageType) {
          hasExpected = true;
          var m = messageList.get(0);
          var isFsr = m.getMessageType() == MessageType.FIELD_SITUATION;
          location = isFsr ? m.msgLocation : m.mapLocation;
          location = location != null && location.isValid() ? location : null;
        } else {
          hasUnexpected = true;
          for (var m : messageList) {
            if (m.mapLocation != null && m.mapLocation.isValid()) {
              location = m.mapLocation;
              break;
            } else {
              location = LocationUtils.binaryAngularSubdivision(relocationIndex++, LatLongPair.ZERO_ZERO, 10_000);
            }
          }
        }
      } // end loop over types for sender

      var colorName = "";
      if (hasExpected) {
        if (hasUnexpected) {
          ++mixedCount;
          colorName = colorBlue;
        } else {
          ++expectedCount;
          colorName = colorGreen;
        }
      } else {
        ++unexpectedCount;
        colorName = colorRed;
      }

      if (location == null || !location.isValid()) {
        location = LocationUtils.binaryAngularSubdivision(relocationIndex++, LatLongPair.ZERO_ZERO, 10_000d);
      }
      var mapEntry = new MapEntry(sender, "", location, popupText.toString(), colorName);
      mapEntries.add(mapEntry);
    } // end loop over senders

    var layers = new ArrayList<MapLayer>();
    layers.add(new MapLayer("Only " + exerciseMessageType.name() + " messages, count: " + expectedCount, colorGreen));
    layers.add(
        new MapLayer("Both " + exerciseMessageType.name() + " and other messages, count: " + mixedCount, colorBlue));
    layers.add(new MapLayer("Only other messages, count: " + unexpectedCount, colorRed));

    var legendTitle = dateString + " Message Type Counts (" + mIdFeedbackMap.values().size() + " total)";
    var context = new MapContext(outputPath, //
        dateString + "-map-messageTypes", // file name
        dateString + " Message Type Counts", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);
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
          summary.getFeedbackCount(), summary.getFeedback(), //
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
