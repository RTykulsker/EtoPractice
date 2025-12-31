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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.practice.generator.PracticeUtils;
import com.surftools.wimp.practice.tools.PracticeGeneratorTool;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor to send Acknowledgement messages via Winlink and map to every
 * message sender
 *
 *
 * @author bobt
 *
 */
public class AcknowledgementProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(AcknowledgementProcessor.class);

  public static final String DASHES = "----------------------------------------------------------------------------------------------\n";

  private static final boolean LAST_LOCATION_WINS = true;

  public static final String ACK_MAP = "ackMap";

  private MessageType expectedMessageType;
  static private Map<String, AckEntry> ackMap; // sender -> AckEntry;
  private List<String> badLocationSenders;

  private static Map<String, ReferenceEntry> referenceMap;
  private static String currentExerciseId;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm);

    var messageTypeString = cm.getAsString(Key.EXPECTED_MESSAGE_TYPES);
    expectedMessageType = MessageType.fromString(messageTypeString);
    if (expectedMessageType == null) {
      throw new IllegalArgumentException("unknown messageType: " + messageTypeString);
    }

    badLocationSenders = new ArrayList<>();
    ackMap = new HashMap<>();
    mm.putContextObject(ACK_MAP, ackMap);

    createReferenceMap();
  }

  @Override
  public void process() {
    var senderIterator = mm.getSenderIterator();
    while (senderIterator.hasNext()) { // loop over senders
      var sender = senderIterator.next();
      var ackEntry = new AckEntry(sender);
      for (var m : mm.getAllMessagesForSender(sender)) {
        ackEntry.update(m);
      } // end loop over messages for sender
      ackMap.put(ackEntry.from, ackEntry);
      if (ackEntry.location == null || !ackEntry.location.isValid()) {
        badLocationSenders.add(ackEntry.from);
      }
    } // end loop over senders
  }

  public static String makeText(String from) {
    final var EXPECTED_CONTENT = "Feedback messages and maps for expected message types will be generated and published shortly.\n";
    final var UNEXPECTED_CONTENT = "No feedback can or will be produced for unexpected message types.\n";
    final var EARLY_CONTENT = "No feedback can or will be produced for early messages.\n";
    final var LATE_CONTENT = "No feedback can or will be produced for late messags.\n";
    var expectedContent = cm.getAsString(Key.ACKNOWLEDGEMENT_EXPECTED, EXPECTED_CONTENT);
    var unexpectedContent = cm.getAsString(Key.ACKNOWLEDGEMENT_UNEXPECTED, UNEXPECTED_CONTENT);
    var extraContent = cm.getAsString(Key.ACKNOWLEDGEMENT_EXTRA_CONTENT, "");
    var earlyContent = EARLY_CONTENT;
    var lateContent = LATE_CONTENT;

    var ackEntry = ackMap.get(from);

    var sb = new StringBuilder();
    if (ackEntry.expectedMessageMap.size() > 0) {
      sb.append("The following expected message types are acknowledged:\n");
      sb.append(ackEntry.format(AckType.Expected, 4));
      sb.append("\n");
      sb.append(expectedContent);
      sb.append(extraContent);
    }
    if (ackEntry.unexpectedMessageMap.size() > 0) {
      sb.append("The following unexpected message types are acknowledged:\n");
      sb.append(ackEntry.format(AckType.Unexpected, 4));
      sb.append("\n");
      sb.append(unexpectedContent);
      sb.append(extraContent);
    }
    if (ackEntry.earlyMessageMap.size() > 0) {
      sb.append("The following early message types are acknowledged:\n");
      sb.append(ackEntry.format(AckType.Early, 4));
      sb.append("\n");
      sb.append(earlyContent);
      sb.append(extraContent);
    }
    if (ackEntry.lateMessageMap.size() > 0) {
      sb.append("The following late message types are acknowledged:\n");
      sb.append(ackEntry.format(AckType.Late, 4));
      sb.append("\n");
      sb.append(lateContent);
      sb.append(extraContent);
    }
    sb.append(extraContent);
    sb.append(DASHES);
    sb.append("\n");
    var text = sb.toString();
    return text;
  }

  @Override
  public void postProcess() {
    if (badLocationSenders.size() > 0) {
      logger.info("adjusting lat/long for " + badLocationSenders.size() + " messages from: "
          + String.join(",", badLocationSenders));
      var newLocations = LocationUtils.jitter(badLocationSenders.size(), LatLongPair.ZERO_ZERO, 10_000);
      for (int i = 0; i < badLocationSenders.size(); ++i) {
        var from = badLocationSenders.get(i);
        var ackEntry = ackMap.get(from);
        var newLocation = newLocations.get(i);
        ackEntry.update(newLocation);
        ackMap.put(from, ackEntry);
      }
    }
    mm.putContextObject("ackMap", ackMap);

    var acknowledgments = new ArrayList<AckEntry>(ackMap.values().stream().toList());
    WriteProcessor.writeTable(new ArrayList<IWritableTable>(acknowledgments), "acknowledgements.csv");

    // send acknowledgement messages here, we do it from BasePracticeProcessor
  }

  record ReferenceEntry(LocalDate date, MessageType messageType) {
  };

  public record AckKey(String from, String messageId, MessageType messageType) {
  };

  public enum AckType {
    Expected, Unexpected, Early, Late
  };

  public class AckEntry implements IWritableTable {
    public final String from;
    public LatLongPair location;
    public Map<AckKey, ExportedMessage> expectedMessageMap;
    public Map<AckKey, ExportedMessage> unexpectedMessageMap;
    public Map<AckKey, ExportedMessage> earlyMessageMap;
    public Map<AckKey, ExportedMessage> lateMessageMap;

    public AckEntry(String sender) {
      this.from = sender;

      expectedMessageMap = new HashMap<>();
      unexpectedMessageMap = new HashMap<>();
      earlyMessageMap = new HashMap<>();
      lateMessageMap = new HashMap<>();
    }

    public String getId() {
      return from;
    }

    public void update(ExportedMessage m) {
      // de-duplicate identical messages, support multiple messages of same type
      var ackKey = new AckKey(m.from, m.messageId, m.getMessageType());
      var exerciseId = getExerciseId(m.getPlainContent().toLowerCase());
      var isExpected = expectedMessageType == m.getMessageType() && exerciseId.equals(currentExerciseId);
      if (isExpected) {
        expectedMessageMap.put(ackKey, m);
      } else {
        if (exerciseId.length() > 0) { // early or late
          var refEntry = referenceMap.get(exerciseId);
          if (refEntry == null) {
            unexpectedMessageMap.put(ackKey, m);
          } else { //
            var refDate = refEntry.date;
            if (date.isBefore(refDate)) {
              earlyMessageMap.put(ackKey, m);
            } else {
              lateMessageMap.put(ackKey, m);
            }
          }
        } else {
          unexpectedMessageMap.put(ackKey, m);
        }
      }

      if (LAST_LOCATION_WINS) {
        this.location = m.mapLocation;
      } else {
        if (this.location == null) {
          this.location = m.mapLocation;
        }
      }
    }

    public void update(LatLongPair newLocation) {
      this.location = newLocation;
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (AckEntry) other;
      return from.compareTo(o.from);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "From", "Latitude", "Longitude", //
          "Expected Messages", "Unexpected Messages", //
          "Early Messages", "Late Messages", //
          "Expected Message #", "Unexpected Message #", //
          "Early #", "Late #", //
          "Total Message #" };
    }

    @Override
    public String[] getValues() {
      return new String[] { from, location.getLatitude(), location.getLongitude(), //
          format(AckType.Expected, 5), format(AckType.Unexpected, 5), //
          format(AckType.Early, 5), format(AckType.Late, 5), //
          s(expectedMessageMap.size()), s(unexpectedMessageMap.size()), //
          s(earlyMessageMap.size()), s(lateMessageMap.size()), //
          s(expectedMessageMap.size() + unexpectedMessageMap.size() + earlyMessageMap.size() + lateMessageMap.size()) //
      };
    }

    private String format(AckType ackType, int formatStyle) {
      final Map<Integer, String> formatMap = Map.of(//
          1, "%s,%s,%s", //
          2, "%s %s %s", //
          3, "Date: %s\nMessageId: %s\nType: %s\n", //
          4, "Date: %s, MessageId: %s, Type: %s", //
          5, "Date: %s, MessageId: %s, Type: %s\n" //
      );

      var formatString = formatMap.get(formatStyle);
      Map<AckKey, ExportedMessage> map = null;
      if (ackType == AckType.Expected) {
        map = expectedMessageMap;
      } else if (ackType == AckType.Unexpected) {
        map = unexpectedMessageMap;
      } else if (ackType == AckType.Early) {
        map = earlyMessageMap;
      } else {
        map = lateMessageMap;
      }

      var values = new ArrayList<ExportedMessage>(map.values());
      Collections.sort(values); // by sort time!
      var resultList = new ArrayList<String>();
      for (var m : values) {
        var aResult = String.format(formatString, DTF.format(m.msgDateTime), m.messageId,
            m.getMessageType().toString());
        resultList.add(aResult);
      }
      var results = String.join("\n", resultList);
      return results;
    }

  }

  private void createReferenceMap() {
    var referencePathName = cm.getAsString(Key.PATH_REFERENCE);
    referenceMap = new HashMap<>();

    var referencePath = Path.of(referencePathName);
    try (Stream<Path> stream = Files.walk(referencePath)) {
      stream.filter(Files::isRegularFile).forEach(AcknowledgementProcessor::processAcknowledgementFile);
    } catch (Exception e) {
      logger.error("Exception processing reference path: " + referencePathName + ", " + e.getMessage());
    }

    logger.info("read " + referenceMap.size() + " reference files");
  }

  private static void processAcknowledgementFile(Path path) {
    var fileName = path.getFileName().toString();
    if (!fileName.endsWith(".txt")) {
      return;
    }
    logger.debug("processing reference file: " + fileName);

    try {
      var anExerciseDateString = fileName.substring(0, 10);
      var anExerciseDate = LocalDate.parse(anExerciseDateString);
      var ord = PracticeUtils.getOrdinalDayOfWeek(anExerciseDate);
      var messageType = PracticeGeneratorTool.MESSAGE_TYPE_MAP.get(ord);

      var content = Files.readString(path).toLowerCase();
      var exerciseId = getExerciseId(content);

      var referenceEntry = new ReferenceEntry(anExerciseDate, messageType);
      referenceMap.put(exerciseId, referenceEntry);

      if (anExerciseDateString.equals(dateString)) {
        currentExerciseId = exerciseId;
      }
    } catch (Exception e) {
      logger.error("Exception reading reference file: " + fileName + ", " + e.getMessage());
    }

  }

  private static String getExerciseId(String content) {
    final var key = "exercise id: ";
    var index = content.indexOf(key);
    var exerciseId = "";
    if (index != -1) {
      index = index + key.length();
      exerciseId = content.substring(index, index + 12);
    }
    return exerciseId;
  }

}
