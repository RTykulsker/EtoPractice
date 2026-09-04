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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.surftools.utils.BucketChooser;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.message.Ics213RRMessage.LineItem;
import com.surftools.wimp.schedule.ScheduleRecord;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class Ics213RrGenerator extends AbstractBasePracticeGenerator {

  private static final String SANTAS_WISHLIST = "Santa's Wishlist";
  private static final String SANTA = "SANTA";
  private static boolean isInitialized = false;
  private BucketChooser<ResourceItem> santaChooser;
  private BucketChooser<BucketChooser<ResourceItem>> resourceChooserChooser;
  private Random dateRng;

  @Override
  public void initialize(IConfigurationManager cm) {
    super.initialize(cm);

    if (!isInitialized) {
      initialize();
      isInitialized = true;
    }

  }

  private void initialize() {
    var resourceListMap = new HashMap<String, ArrayList<ResourceItem>>();
    for (var resource : resourceList) {
      var key = resource.key();
      var list = resourceListMap.getOrDefault(key, new ArrayList<ResourceItem>());
      list.add(resource);
      resourceListMap.put(key, list);
    }

    santaChooser = new BucketChooser<ResourceItem>(santaList, baseRng);

    var chooserList = new ArrayList<BucketChooser<ResourceItem>>();
    for (var key : resourceListMap.keySet()) {
        var list = resourceListMap.get(key);
        var chooser = new BucketChooser<ResourceItem>(list, baseRng);
        chooserList.add(chooser);      
    }
    resourceChooserChooser = new BucketChooser<BucketChooser<ResourceItem>>(chooserList, baseRng);
  }

  @Override
  public Ics213RRMessage generateMessage(LocalDate date, ScheduleRecord schedule) {
    dateRng = getRandom(date.toString());
    final int nLineItems = 3;
    Ics213RRMessage.setLineItemsToDisplay(nLineItems);

    var incidentName = data.getExerciseId(date);
    var requestNumber = data.getExerciseId(date);
    var subject = "ICS 213RR- " + incidentName + "- Request #:" + requestNumber;
    var exportedMessage = makeExportedMessage(date, subject);
    var organization = "EmComm Training Organization";
    var lineItems = getLineItems(date, nLineItems, null, null, schedule);
    var delivery = data.deliveryChooser.next();
    var substitutes = dateRng.nextBoolean() ? "substitute as appropriate" : "no substitutes allowed";
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
  public String generateIntructions(ExportedMessage message, LocalDate date, ScheduleRecord schedule) {
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

  public record ResourceItem(String key, String qty, String kind, String type, String description) {
    public static ResourceItem fromArray(String[] array) {
      return new ResourceItem(array[0], array[1], array[2], array[3], array[4]);
    }
  };

  private List<LineItem> getLineItems(LocalDate date, int desiredCount, Integer minInt, Integer maxInt,
      ScheduleRecord schedule) {
    var lineItems = new ArrayList<LineItem>(desiredCount);
    var chooser = (schedule.extraData().equals(SANTAS_WISHLIST) || schedule.extraData().equals(SANTA)) //
    		? santaChooser : resourceChooserChooser.next();
    var minQty = minInt == null ? 1 : minInt.intValue();
    var maxQty = maxInt == null ? 100 : maxInt.intValue();

    var timeString = dateRng.nextInt(10, 18) + ":00";

    for (var i = 0; i < Ics213RRMessage.MAX_LINE_ITEMS; ++i) {
      if (i < desiredCount) {
        var resource = chooser.next();
        if (resource.qty.equals("0")) {
          var qty = String.valueOf(dateRng.nextInt(minQty, maxQty));
          resource = new ResourceItem(resource.key, qty, resource.kind, resource.type, resource.description);
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
  
  List<ResourceItem> santaList = List.of(
		    new ResourceItem("SANTA","1","n/a","n/a","Wolf River Silver Bullet 1000"),
		    new ResourceItem("SANTA","1","n/a","n/a","LDG Electronics AT-1000ProII Automatic Antenna Tuner"),
		    new ResourceItem("SANTA","1","n/a","n/a","Heil Sound PRO 7 Headset"),
		    new ResourceItem("SANTA","2","n/a","n/a","Bioenno Power BLF-1220A LiFePO4 Battery"),
		    new ResourceItem("SANTA","1","n/a","n/a","RigExpert Antenna Analyzer AA-55ZOOM"),
		    new ResourceItem("SANTA","1","n/a","n/a","Kenwood TS-990S HF/6 Meter Base Transceiver")
		    );

  List<ResourceItem> resourceList = List.of(
		    new ResourceItem("Earthquake","1","Team","Type 1","Type 1 USAR Task Force for Heavy rescue for collapsed structures"),
		    new ResourceItem("Earthquake","3","Persnl","Type 1","Type 1 Structural Engineers for Rapid building safety assessment"),
		    new ResourceItem("Earthquake","2","Equipmnt","Type 1","Type 1 Heavy Rescue Truck for Confined-space rescue"),
		    new ResourceItem("Earthquake","4","Persnl","Type 1","Type 1 Paramedics for Medical support for triage"),
		    new ResourceItem("Earthquake","1","Equipmnt","Type 1","Type 1 Mobile Communications Unit for ICP communications"),
		    new ResourceItem("Earthquake","2","Equipmnt","Type 1","Type 1 Generator (100 kW) for Power for shelters"),
		    new ResourceItem("Earthquake","5","Persnl","Type 2","Type 2 Public Works Crew for Debris removal"),
		    new ResourceItem("Earthquake","1","Equipmnt","Type 1","Type 1 Mobile Field Kitchen for Feeding 150 responders"),
		    new ResourceItem("Earthquake","2","Equipmnt","Type 2","Type 2 Dump Truck for Debris transport"),
		    new ResourceItem("Earthquake","1","Team","Type 3","Type 3 IMT for Incident management"),
		    new ResourceItem("Earthquake","3","Equipmnt","Type 1","Type 1 Light Tower for Night operations"),
		    new ResourceItem("Earthquake","2","Equipmnt","Type 1","Type 1 Air Monitoring Unit for Air quality monitoring"),
		    new ResourceItem("Earthquake","1","Equipmnt","Type 1","Type 1 Mobile Staging Package for Resource staging"),
		    new ResourceItem("Earthquake","4","Persnl","Type 1","Type 1 Heavy Equipment Operators for Rubble clearing"),
		    new ResourceItem("Earthquake","2","Equipmnt","Type 1","Type 1 Thermal Imaging Camera for Interior search"),
		    new ResourceItem("Earthquake","1","Equipmnt","Type 1","Type 1 Mobile Weather Station for Weather monitoring"),
		    new ResourceItem("Earthquake","2","Equipmnt","Type 1","Type 1 Rescue Boat for Canal collapse rescue"),
		    new ResourceItem("Earthquake","3","Persnl","Type 2","Type 2 Logistics Staff for Supply distribution"),
		    new ResourceItem("Earthquake","1","Team","Type 2","Type 2 Search Team for Canine search"),
		    new ResourceItem("Earthquake","2","Equipmnt","Type 1","Type 1 Air Support Unit for Drone mapping"),

		    new ResourceItem("Wildfire","2","Equipmnt","Type 3","Type 3 Engine (E-3) for Initial attack"),
		    new ResourceItem("Wildfire","2","Equipmnt","Type 2","Type 2 Dozer for Line construction"),
		    new ResourceItem("Wildfire","1","Equipmnt","Type 1","Type 1 Water Tender for Water supply"),
		    new ResourceItem("Wildfire","4","Persnl","Type 1","Type 1 Wildland Firefighters for Hand crew"),
		    new ResourceItem("Wildfire","1","Equipmnt","Type 1","Type 1 Air Support Unit for Drone fire mapping"),
		    new ResourceItem("Wildfire","2","Equipmnt","Type 1","Type 1 Thermal Imaging Camera for Hot spot detection"),
		    new ResourceItem("Wildfire","1","Team","Type 1","Type 1 IMT for Incident management"),
		    new ResourceItem("Wildfire","3","Persnl","Type 2","Type 2 Public Works Crew for Road clearing"),
		    new ResourceItem("Wildfire","1","Equipmnt","Type 1","Type 1 Mobile Communications Unit for ICP comms"),
		    new ResourceItem("Wildfire","2","Equipmnt","Type 1","Type 1 Generator for Base camp power"),
		    new ResourceItem("Wildfire","1","Equipmnt","Type 1","Type 1 Mobile Field Kitchen for Feeding 200 personnel"),
		    new ResourceItem("Wildfire","2","Equipmnt","Type 1","Type 1 Light Tower for Night ops"),
		    new ResourceItem("Wildfire","1","Equipmnt","Type 1","Type 1 Fuel Tender for On-scene fueling"),
		    new ResourceItem("Wildfire","3","Persnl","Type 1","Type 1 Paramedics for Medical support"),
		    new ResourceItem("Wildfire","1","Team","Type 2","Type 2 HazMat Team for Smoke plume analysis"),
		    new ResourceItem("Wildfire","2","Equipmnt","Type 1","Type 1 Air Monitoring Unit for Smoke monitoring"),
		    new ResourceItem("Wildfire","3","Equipmnt","Type 2","Type 2 Dump Truck for Debris transport"),
		    new ResourceItem("Wildfire","1","Equipmnt","Type 1","Type 1 Mobile Weather Station for Fire weather monitoring"),
		    new ResourceItem("Wildfire","2","Equipmnt","Type 1","Type 1 Rescue Boat for River corridor protection"),
		    new ResourceItem("Wildfire","4","Persnl","Type 2","Type 2 Logistics Staff for Supply distribution"),

		    new ResourceItem("Tornado","1","Team","Type 1","Type 1 USAR Task Force for Wide-area collapse rescue"),
		    new ResourceItem("Tornado","3","Persnl","Type 1","Type 1 Structural Engineers for Damage assessment"),
		    new ResourceItem("Tornado","2","Equipmnt","Type 1","Type 1 Heavy Rescue Truck for Extrication"),
		    new ResourceItem("Tornado","4","Persnl","Type 1","Type 1 Paramedics for Medical surge"),
		    new ResourceItem("Tornado","1","Equipmnt","Type 1","Type 1 Mobile Communications Unit for ICP comms"),
		    new ResourceItem("Tornado","2","Equipmnt","Type 1","Type 1 Generator for Power restoration"),
		    new ResourceItem("Tornado","5","Persnl","Type 2","Type 2 Public Works Crew for Debris clearing"),
		    new ResourceItem("Tornado","1","Equipmnt","Type 1","Type 1 Mobile Field Kitchen for Feeding responders"),
		    new ResourceItem("Tornado","2","Equipmnt","Type 2","Type 2 Dump Truck for Debris hauling"),
		    new ResourceItem("Tornado","1","Team","Type 3","Type 3 IMT for Incident management"),
		    new ResourceItem("Tornado","3","Equipmnt","Type 1","Type 1 Light Tower for Night ops"),
		    new ResourceItem("Tornado","2","Equipmnt","Type 1","Type 1 Air Monitoring Unit for Air quality"),
		    new ResourceItem("Tornado","1","Equipmnt","Type 1","Type 1 Mobile Staging Package for Staging support"),
		    new ResourceItem("Tornado","4","Persnl","Type 1","Type 1 Heavy Equipment Operators for Rubble removal"),
		    new ResourceItem("Tornado","2","Equipmnt","Type 1","Type 1 Thermal Imaging Camera for Search ops"),
		    new ResourceItem("Tornado","1","Equipmnt","Type 1","Type 1 Mobile Weather Station for Weather monitoring"),
		    new ResourceItem("Tornado","2","Equipmnt","Type 1","Type 1 Rescue Boat for Waterway debris"),
		    new ResourceItem("Tornado","3","Persnl","Type 2","Type 2 Logistics Staff for Supply distribution"),
		    new ResourceItem("Tornado","1","Team","Type 2","Type 2 Search Team for Canine search"),
		    new ResourceItem("Tornado","2","Equipmnt","Type 1","Type 1 Air Support Unit for Drone mapping"),

		    new ResourceItem("Flood","2","Equipmnt","Type 1","Type 1 Swiftwater Rescue Boat for Rescue operations"),
		    new ResourceItem("Flood","4","Persnl","Type 1","Type 1 Water Rescue Technicians for Swiftwater rescue"),
		    new ResourceItem("Flood","1","Equipmnt","Type 1","Type 1 High-Water Vehicle for Evacuation"),
		    new ResourceItem("Flood","2","Equipmnt","Type 1","Type 1 Rescue Boat for Urban water rescue"),
		    new ResourceItem("Flood","1","Equipmnt","Type 1","Type 1 Mobile Communications Unit for ICP comms"),
		    new ResourceItem("Flood","2","Equipmnt","Type 1","Type 1 Generator for Shelter power"),
		    new ResourceItem("Flood","5","Persnl","Type 2","Type 2 Public Works Crew for Debris clearing"),
		    new ResourceItem("Flood","1","Equipmnt","Type 1","Type 1 Mobile Field Kitchen for Feeding evacuees"),
		    new ResourceItem("Flood","2","Equipmnt","Type 2","Type 2 Dump Truck for Debris hauling"),
		    new ResourceItem("Flood","1","Team","Type 3","Type 3 IMT for Incident management"),
		    new ResourceItem("Flood","3","Equipmnt","Type 1","Type 1 Light Tower for Night ops"),
		    new ResourceItem("Flood","2","Equipmnt","Type 1","Type 1 Air Monitoring Unit for Air quality"),
		    new ResourceItem("Flood","1","Equipmnt","Type 1","Type 1 Mobile Staging Package for Staging support"),
		    new ResourceItem("Flood","4","Persnl","Type 1","Type 1 Heavy Equipment Operators for Road clearing"),
		    new ResourceItem("Flood","2","Equipmnt","Type 1","Type 1 Thermal Imaging Camera for Search ops"),
		    new ResourceItem("Flood","1","Equipmnt","Type 1","Type 1 Mobile Weather Station for Weather monitoring"),
		    new ResourceItem("Flood","2","Equipmnt","Type 1","Type 1 Rescue Boat for Waterway patrol"),
		    new ResourceItem("Flood","3","Persnl","Type 2","Type 2 Logistics Staff for Supply distribution"),
		    new ResourceItem("Flood","1","Team","Type 2","Type 2 Search Team for Canine search"),
		    new ResourceItem("Flood","2","Equipmnt","Type 1","Type 1 Air Support Unit for Drone mapping"),

		    new ResourceItem("Winter Storm","2","Equipmnt","Type 1","Type 1 Snowcat for Remote access"),
		    new ResourceItem("Winter Storm","4","Persnl","Type 1","Type 1 Paramedics for Medical support"),
		    new ResourceItem("Winter Storm","1","Equipmnt","Type 1","Type 1 Mobile Communications Unit for ICP comms"),
		    new ResourceItem("Winter Storm","2","Equipmnt","Type 1","Type 1 Generator for Shelter power"),
		    new ResourceItem("Winter Storm","5","Persnl","Type 2","Type 2 Public Works Crew for Snow clearing"),
		    new ResourceItem("Winter Storm","1","Equipmnt","Type 1","Type 1 Mobile Field Kitchen for Feeding evacuees"),
		    new ResourceItem("WinterStorm","2","Equipmnt","Type 2","Type 2 Dump Truck for Snow hauling"),
		    new ResourceItem("Winter Storm","1","Team","Type 3","Type 3 IMT for Incident management"),
		    new ResourceItem("Winter Storm","3","Equipmnt","Type 1","Type 1 Light Tower for Night ops"),
		    new ResourceItem("Winter Storm","2","Equipmnt","Type 1","Type 1 Air Monitoring Unit for Air quality"),
		    new ResourceItem("Winter Storm","1","Equipmnt","Type 1","Type 1 Mobile Staging Package for Staging support"),
		    new ResourceItem("Winter Storm","4","Persnl","Type 1","Type 1 Heavy Equipment Operators for Snow removal"),
		    new ResourceItem("Winter Storm","2","Equipmnt","Type 1","Type 1 Thermal Imaging Camera for Search ops"),
		    new ResourceItem("Winter Storm","1","Equipmnt","Type 1","Type 1 Mobile Weather Station for Weather monitoring"),
		    new ResourceItem("Winter Storm","2","Equipmnt","Type 1","Type 1 Rescue Boat for Frozen waterway rescue"),
		    new ResourceItem("Winter Storm","3","Persnl","Type 2","Type 2 Logistics Staff for Supply distribution"),
		    new ResourceItem("Winter Storm","1","Team","Type 2","Type 2 Search Team for Canine search"),
		    new ResourceItem("Winter Storm","2","Equipmnt","Type 1","Type 1 Air Support Unit for Drone mapping")
		);

}
