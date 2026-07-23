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

package com.surftools.wimp.practice.generator;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.surftools.utils.BucketChooser;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.message.Ics213RRMessage.LineItem;
import com.surftools.wimp.practice.generator.PracticeData.ExerciseIdMethod;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class Ics213RrGenerator extends AbstractBasePracticeGenerator {

  private static boolean isInitialized = false;
  private static final String SANTA_KEY = "SANTA";
  private BucketChooser<ResourceEntry> santaChooser;
  private BucketChooser<BucketChooser<ResourceEntry>> resourceChooserChooser;

  @Override
  public void initialize(IConfigurationManager cm) {
    super.initialize(cm);

    if (!isInitialized) {
      initialize();
      isInitialized = true;
    }

  }

  private void initialize() {
    var dataPathName = cm.getAsString(Key.PATH_RESOUCE_CONTENT);
    var dataFilePath = Path.of(dataPathName);

    var resourceList = new ArrayList<ResourceEntry>();
    try {
      var listOfStringArrays = ReadProcessor.readCsvFileIntoFieldsArray(dataFilePath);
      for (var array : listOfStringArrays) {
        var key = array[0];
        if (key.equals("Key")) {
          continue;
        }

        var resource = ResourceEntry.fromArray(array);
        resourceList.add(resource);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    Collections.sort(resourceList, Comparator.comparing(ResourceEntry::key));

    var resourceListMap = new HashMap<String, ArrayList<ResourceEntry>>();
    for (var resource : resourceList) {
      var key = resource.key();
      var list = resourceListMap.getOrDefault(key, new ArrayList<ResourceEntry>());
      list.add(resource);
      resourceListMap.put(key, list);
    }

    var chooserList = new ArrayList<BucketChooser<ResourceEntry>>();
    for (var key : resourceListMap.keySet()) {
      var list = resourceListMap.get(key);
      if (key.equals(SANTA_KEY)) {
        santaChooser = new BucketChooser<ResourceEntry>(list, rng);
      } else {
        var chooser = new BucketChooser<ResourceEntry>(list, rng);
        chooserList.add(chooser);
      }
    }
    resourceChooserChooser = new BucketChooser<BucketChooser<ResourceEntry>>(chooserList, rng);
  }

  @Override
  public Ics213RRMessage generateMessage(LocalDate date) {
    final int nLineItems = 3;
    Ics213RRMessage.setLineItemsToDisplay(nLineItems);

    var incidentName = "ETO Weekly Practice";
    var requestNumber = "Exercise Id: " + data.getExerciseId(ExerciseIdMethod.PHONE);
    var subject = "ICS 213RR- " + incidentName + "- Request #:" + requestNumber;
    var exportedMessage = makeExportedMessage(date, subject);
    var organization = "EmComm Training Organization";
    var lineItems = getLineItems(date, nLineItems, null, null);
    var delivery = data.deliveryChooser.next();
    var substitutes = rng.nextBoolean() ? "substitute as appropriate" : "no substitutes allowed";
    var requestedBy = data.doubleNameChooser.next() + " / " + data.shortRoleChooser.next();
    var priority = data.priorityChooser.next();
    var approvedBy = data.doubleNameChooser.next();
    var version = NA;
    var expressVersion = NA;

    var m = new Ics213RRMessage(exportedMessage, organization, incidentName, //
        NA, requestNumber, //
        lineItems, //
        delivery, substitutes, requestedBy, priority, approvedBy, //
        "", "", "", // logisticsOrderNumber, supplierInfo, supplierName, //
        "", "", "", // supplierPointOfContact, supplyNotes, logisticsAuthorizer, //
        "", "", // logisticsDateTime, orderedBy, //
        "", "", "", // financeComments, financeName, financeDateTime//
        version, expressVersion);

    return m;
  }

  @Override
  public String generateIntructions(ExportedMessage message, LocalDate date) {
    var m = (Ics213RRMessage) message;

    var sb = new StringBuilder();
    sb.append(generateInstructionHeader(date, "Complete an ICS-213 Resource Request Message"));

    sb.append(INDENT + "Setup: agency or group name: " + m.organization + NL);
    sb.append(INDENT + "Incident name: " + m.incidentName + NL);
    sb.append(INDENT + "Date/Time: (click in box and accept date/time)" + NL);
    sb.append(INDENT + "Resource Request Number: " + m.requestNumber + NL);
    sb.append(INDENT + "Order Items (leave Estimated and Cost empty)" + NL);

    var lineNumber = 0;
    for (var line : m.lineItems) {
      if (line.isEmpty()) {
        continue;
      }

      ++lineNumber;
      sb.append(INDENT2 + "line " + lineNumber + NL); //
      sb.append(INDENT3 + "Qty: " + line.quantity() + NL);
      sb.append(INDENT3 + "Kind: " + line.kind() + NL);
      sb.append(INDENT3 + "Type: " + line.type() + NL);
      sb.append(INDENT3 + "Description: " + line.item() + NL);
      sb.append(INDENT3 + "Requested Time: " + line.requestedDateTime() + NL);
    }

    sb.append(INDENT + "Delivery/Reporting Location: " + m.delivery + NL);
    sb.append(INDENT + "Substitutes: " + m.substitutes + NL);
    sb.append(INDENT + "Requested by Name/Position: " + m.requestedBy + NL);
    sb.append(INDENT + "Priority: " + m.priority + NL);
    sb.append(INDENT + "Section Chief Name for Approval: " + m.approvedBy + NL);

    sb.append(generateInstructionTail());
    return sb.toString();
  }

  public record ResourceEntry(String key, String qty, String kind, String type, String description) {
    public static ResourceEntry fromArray(String[] array) {
      return new ResourceEntry(array[0], array[1], array[2], array[3], array[4]);
    }
  };

  private List<LineItem> getLineItems(LocalDate date, int desiredCount, Integer minInt, Integer maxInt) {
    var lineItems = new ArrayList<LineItem>(desiredCount);
    var chooser = (date.getMonth() == Month.DECEMBER) ? santaChooser : resourceChooserChooser.next();
    var minQty = minInt == null ? 1 : minInt.intValue();
    var maxQty = maxInt == null ? 100 : maxInt.intValue();

    var timeString = rng.nextInt(10, 18) + ":00";

    for (var i = 0; i < Ics213RRMessage.MAX_LINE_ITEMS; ++i) {
      if (i < desiredCount) {
        var resource = chooser.next();
        if (resource.qty.equals("0")) {
          var qty = String.valueOf(rng.nextInt(minQty, maxQty));
          resource = new ResourceEntry(resource.key, qty, resource.kind, resource.type, resource.description);
        }
        var lineItem = new LineItem(resource.qty, resource.kind, resource.type, resource.description, //
            timeString, "", "");
        lineItems.add(lineItem);
      } else {
        lineItems.add(LineItem.EMPTY);
      }
    }
    return lineItems;
  }
}
