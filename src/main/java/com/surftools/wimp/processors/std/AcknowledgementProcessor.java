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

package com.surftools.wimp.processors.std;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
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
  public static final String DASHES = "----------------------------------------------------------------------------------------------\n";

  private static final boolean LAST_LOCATION_WINS = true;

  public static final String ACK_MAP = "ackMap";

  private MessageType expectedMessageType;
  static private Map<String, AckEntry> ackMap; // sender -> AckEntry;
  private int relocationIndex = 0;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm);

    var messageTypeString = cm.getAsString(Key.EXPECTED_MESSAGE_TYPES);
    expectedMessageType = MessageType.fromString(messageTypeString);
    if (expectedMessageType == null) {
      throw new IllegalArgumentException("unknown messageType: " + messageTypeString);
    }

    ackMap = new HashMap<>();
    mm.putContextObject(ACK_MAP, ackMap);
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
        var location = LocationUtils.binaryAngularSubdivision(relocationIndex, LatLongPair.ZERO_ZERO, 10_000d);
        ackEntry.location = location;
      }
    } // end loop over senders
  }

  public static String makeText(String from) {
    final var EXPECTED_CONTENT = "Feedback messages and maps for expected message types will be generated and published shortly.\n";
    final var UNEXPECTED_CONTENT = "No feedback can or will be produced for unexpected message types.\n";
    var expectedContent = cm.getAsString(Key.ACKNOWLEDGEMENT_EXPECTED, EXPECTED_CONTENT);
    var unexpectedContent = cm.getAsString(Key.ACKNOWLEDGEMENT_UNEXPECTED, UNEXPECTED_CONTENT);
    var extraContent = cm.getAsString(Key.ACKNOWLEDGEMENT_EXTRA_CONTENT, "");

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

    sb.append(extraContent);
    sb.append(DASHES);
    sb.append("\n");
    var text = sb.toString();
    return text;
  }

  @Override
  public void postProcess() {
    mm.putContextObject("ackMap", ackMap);

    var acknowledgments = new ArrayList<AckEntry>(ackMap.values().stream().toList());
    WriteProcessor.writeTable(new ArrayList<IWritableTable>(acknowledgments), "acknowledgements.csv");

    // don't send acknowledgement messages here, we do it from BasePracticeProcessor
  }

  record ReferenceEntry(LocalDate date, MessageType messageType) {
  };

  public record AckKey(String from, String messageId, MessageType messageType) {
  };

  public enum AckType {
    Expected, Unexpected
  };

  public class AckEntry implements IWritableTable {
    public final String from;
    public LatLongPair location;
    public Map<AckKey, ExportedMessage> expectedMessageMap;
    public Map<AckKey, ExportedMessage> unexpectedMessageMap;

    public AckEntry(String sender) {
      this.from = sender;

      expectedMessageMap = new HashMap<>();
      unexpectedMessageMap = new HashMap<>();
    }

    public String getId() {
      return from;
    }

    public void update(ExportedMessage m) {
      // de-duplicate identical messages, support multiple messages of same type
      var ackKey = new AckKey(m.from, m.messageId, m.getMessageType());
      var isExpected = expectedMessageType == m.getMessageType();
      if (isExpected) {
        expectedMessageMap.put(ackKey, m);
      } else {
        unexpectedMessageMap.put(ackKey, m);
      }

      if (LAST_LOCATION_WINS)

      {
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
          "Expected Message #", "Unexpected Message #", //
          "Total Message #" };
    }

    @Override
    public String[] getValues() {
      return new String[] { from, location.getLatitude(), location.getLongitude(), //
          format(AckType.Expected, 5), format(AckType.Unexpected, 5), //
          s(expectedMessageMap.size()), s(unexpectedMessageMap.size()), //
          s(expectedMessageMap.size() + unexpectedMessageMap.size()) //
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

}
