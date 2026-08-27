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
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.surftools.utils.BucketChooser;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Hics251Message;
import com.surftools.wimp.message.Hics251Message.StatusEntry;
import com.surftools.wimp.message.Hics251Message.StatusType;
import com.surftools.wimp.schedule.ScheduleRecord;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class Hics251Generator extends AbstractBasePracticeGenerator {

  private boolean isInitialized = false;
  private BucketChooser<String> hospitalNameChooser;
  private Random dateRng;
  private BucketChooser<String> departmentNameChooser;
  private BucketChooser<StatusType> statusTypeChooser;
  private BucketChooser<String> remarksChooser;
  private Map<String, BucketChooser<String>> commentsChooser;

  @Override
  public void initialize(IConfigurationManager cm) {
    super.initialize(cm);

    if (!isInitialized) {
      hospitalNameChooser = new BucketChooser<String>(PracticeData.hospitalNames, baseRng);
      departmentNameChooser = new BucketChooser<String>(departments, baseRng);
      statusTypeChooser = new BucketChooser<StatusType>(Arrays.asList(StatusType.values()), baseRng);
      remarksChooser = new BucketChooser<String>(remarksList, baseRng);

      commentsChooser = new HashMap<>();
      for (var systemName : Hics251Message.SYSTEM_NAMES) {
        var list = systemCommentsListMap.get(systemName);
        if (list == null) {
          throw new RuntimeException("Could not get list of comments for system: " + systemName);
        }
        var chooser = new BucketChooser<String>(list, baseRng);
        commentsChooser.put(systemName, chooser);
      }

      isInitialized = true;
    }
  }

  @Override
  public Hics251Message generateMessage(LocalDate date, ScheduleRecord schedule) {
    dateRng = getRandom(date.toString());

    var incidentName = data.getExerciseId(date);
    var facilityName = hospitalNameChooser.next();

    // HICS-251 My Incident, my facility, 2026-08-24 19:15
    var subject = "HICS-251 " + incidentName + ", " + facilityName + ", " + date.toString() + " 12:00";
    var exportedMessage = makeExportedMessage(date, subject);
    var pageNumber = String.valueOf(dateRng.nextInt(1, 4));
    var pageTotal = String.valueOf(Integer.parseInt(pageNumber) + dateRng.nextInt(0, 4));

    var operationalPeriod = String.valueOf(dateRng.nextInt(1, 3));
    var windowOpenDate = date.minusDays(5);
    var windowCloseDate = date.plusDays(1);

    var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    var opFromDate = dtf.format(windowOpenDate);
    var opFromTime = "00:00";
    var opToDate = dtf.format(windowCloseDate);
    var opToTime = "08:00";

    var departmentName = departmentNameChooser.next();
    var hospitalEntry = data.hospitalEntryChooser.next();
    var contactNumber = hospitalEntry.contactPhone();
    var streetAddress = dateRng.nextInt(1, 10) + hospitalEntry.address();
    var city = hospitalEntry.city();
    var state = hospitalEntry.state();
    var zip = hospitalEntry.zip();
    var formLocation = new LatLongPair(hospitalEntry.latitude(), hospitalEntry.longitude());

    var statusEntries = makeStatusEntryMap(departmentName);
    var remarks = remarksChooser.next();
    var preparedBy = data.nameChooser.next();

    var m = new Hics251Message(exportedMessage, //
        incidentName, String.valueOf(pageNumber), String.valueOf(pageTotal), //
        operationalPeriod, opFromDate, opFromTime, opToDate, opToTime, //
        departmentName, contactNumber, //
        streetAddress, city, state, zip, //
        statusEntries, //
        remarks, //
        preparedBy, NA, facilityName, //
        NA, formLocation, //
        NA, NA);

    return m;
  }

  @Override
  public String generateIntructions(ExportedMessage message, LocalDate date, ScheduleRecord schedule) {
    var m = (Hics251Message) message;

    var sb = new StringBuilder(); // exercise instructions
    sb.append("Task: Complete a HICS 251 – FACILITY SYSTEM STATUS REPORT Message" + NL + NL);
    sb.append(INDENT + "Incident name: " + m.incidentName + NL);
    sb.append(INDENT + "Page: " + m.pageNumber + " Of " + m.pageTotal + NL);
    sb.append(NL);

    sb.append(INDENT + "Operational Period #: " + m.operationalPeriod + NL);
    sb.append(INDENT + "Operational Period Date From: " + m.opFromDate + NL);
    sb.append(INDENT + "Operational Period Date To: " + m.opToDate + NL);
    sb.append(INDENT + "Operational Period Time From: " + m.opFromTime + NL);
    sb.append(INDENT + "Operational Period Time To: " + m.opToTime + NL);
    sb.append(NL);

    sb.append(INDENT + "Name of Department: " + m.departmentName + NL);
    sb.append(INDENT + "Contact Number: " + m.contactNumber + NL);
    sb.append(INDENT + "Street Address: " + m.streetAddress + NL);
    sb.append(INDENT + "City: " + m.city + NL);
    sb.append(INDENT + "State: " + m.state + NL);
    sb.append(INDENT + "Zip: " + m.zip + NL);

    sb.append(NL);

    for (var systemName : m.statusEntryMap.keySet()) {
      var statusEntry = m.statusEntryMap.get(systemName);
      sb.append(INDENT + systemName + " System" + NL);
      sb.append(INDENT2 + systemName + " Status: " + statusEntry.status() + NL);
      sb.append(INDENT2 + systemName + " Comments: " + statusEntry.comments() + NL);
    }
    sb.append(NL);

    sb.append(INDENT + "Remarks: " + m.remarks + NL);
    sb.append(INDENT + "Prepared by: " + m.preparedBy + NL);
    sb.append(INDENT + "Date / Time: (click in box and accept date/time)" + NL);
    sb.append(INDENT + "Facility Name: " + m.facilityName + NL);

    sb.append(INDENT + "Radio Operator: <YOUR CALL>" + NL);
    sb.append(INDENT + "Facility Latitude: " + m.formLocation.getLatitude() + NL);
    sb.append(INDENT + "Facility Longitude: " + m.formLocation.getLongitude() + NL);
    sb.append(generateInstructionTail());

    return sb.toString();
  }

  public Map<String, StatusEntry> makeStatusEntryMap(String departmentName) {
    var map = new LinkedHashMap<String, StatusEntry>(Hics251Message.SYSTEM_NAMES.size());
    for (var systemName : Hics251Message.SYSTEM_NAMES) {
      var statusType = statusTypeChooser.next();
      var comments = statusType.generatesComments() ? commentsChooser.get(systemName).next() : "";
      map.put(systemName, new Hics251Message.StatusEntry(systemName, statusType, comments));
    }
    return map;
  }

  List<String> departments = List.of("Emergency Medicine", "Cardiology", "Neurology", "Orthopedics", "Pediatrics",
      "Oncology", "Radiology", "Pathology", "Anesthesiology", "Dermatology", "Gastroenterology", "Urology",
      "Nephrology", "Obstetrics and Gynecology", "General Surgery", "Psychiatry", "Pulmonology", "Infectious Diseases",
      "Endocrinology", "Pharmacy");

  List<String> remarksList = List.of("Skilled staffing shortages in critical units",
      "Fuel supply duration for backup generators", "Aging HVAC systems requiring major repairs",
      "Limited surge capacity during mass‑casualty events", "Insufficient backup power for all clinical areas",
      "Delayed vendor delivery for medical supplies", "Water supply interruption risks",
      "Elevator reliability and maintenance backlog", "Insufficient negative‑pressure rooms",
      "IT network redundancy gaps", "Cybersecurity vulnerabilities in legacy systems",
      "Medication stockpile depletion during emergencies", "Insufficient on‑site food reserves for extended events",
      "Structural integrity concerns in older wings", "Limited isolation capacity for infectious disease outbreaks",
      "Staff fatigue and burnout during prolonged incidents", "Inadequate emergency transportation resources",
      "Communication system interoperability issues", "Generator load testing overdue",
      "Roof integrity concerns during severe weather", "Insufficient PPE reserves for high‑demand periods",
      "Delayed biomedical equipment servicing", "Limited cold‑chain storage for critical medications",
      "Potential oxygen supply disruptions", "Parking congestion affecting emergency access",
      "Insufficient shelter‑in‑place supplies", "Hazardous materials storage compliance gaps",
      "Flooding vulnerability in basement mechanical rooms", "Limited telemedicine capacity during surges",
      "Aging fire suppression systems", "Staff credentialing delays during rapid onboarding",
      "Insufficient backup communication devices", "Potential supply chain disruptions for pharmaceuticals",
      "Inadequate waste disposal capacity during high census", "Cooling tower maintenance overdue",
      "Limited redundancy for imaging equipment", "Potential contamination risks in water distribution",
      "Insufficient security staffing during high‑risk events", "Aging patient transport equipment",
      "Limited emergency triage space", "Potential failure points in medical gas distribution",
      "Delayed replacement of worn flooring in clinical areas", "Insufficient training on new emergency protocols",
      "Backup server capacity nearing limits", "Limited decontamination capability for large incidents",
      "Potential radio communication dead zones", "Insufficient lighting in external evacuation routes",
      "Delayed repairs to stormwater drainage systems", "Limited capacity for long‑term sheltering of staff");

  // --- POWER (10) ---
  List<String> powerComments = List.of("breaker panel overheating – estimated 6-hour remediation", //
      "transformer vibration anomaly – estimated 1-day inspection", //
      "intermittent backup circuit drop – estimated 4-hour diagnostic", //
      "fuel pump cavitation – estimated 8-hour repair", //
      "UPS battery degradation – estimated 5-hour replacement", //
      "power conditioner failure – estimated 1-day service", //
      "phase imbalance detected – estimated 6-hour correction", //
      "inverter fault – estimated 3-hour swap", //
      "PDU overheating – estimated 4-hour replacement", //
      "exterior outlet failure – estimated 2-hour fix"//
  );

  // --- LIGHTING (10) ---
  List<String> lightingComments = List.of("flickering triage lights – estimated 3-hour ballast replacement", //
      "LED panel outage – estimated 4-hour fixture swap", //
      "emergency lighting failure – estimated 6-hour repair", //
      "dim hallway lights – estimated 2-hour driver replacement", //
      "motion sensor malfunction – estimated 3-hour recalibration", //
      "floodlight burnout – estimated 2-hour replacement", //
      "emergency signage outage – estimated 2-hour fix", //
      "ceiling light short – estimated 4-hour rewire", //
      "overhead fixture flicker – estimated 3-hour repair", //
      "corridor lighting control fault – estimated 5-hour service");

  // --- WATER (10) ---
  List<String> waterComments = List.of("circulation pump leak – estimated 6-hour replacement", //
      "purification pump failure – estimated 2-day repair", //
      "steam line leak – estimated 4-hour patch", //
      "condensate line leak – estimated 1-day repair", //
      "pressure drop at hand-wash station – estimated 3-hour valve replacement", //
      "sink backup – estimated 5-hour drain clearing", //
      "warm-water mixer fault – estimated 4-hour repair", //
      "cooling loop imbalance – estimated 1-day correction", //
      "shower valve seizure – estimated 3-hour replacement", //
      "exterior spigot leak – estimated 2-hour fix"//
  );

  // --- SEWER / TOILET (10) ---
  List<String> sewerComments = List.of(//
      "flush valve failure – estimated 2-hour repair", //
      "slow sewer drainage – estimated 1-day clearing", //
      "floor drain clog – estimated 4-hour service", //
      "bathroom backup – estimated 6-hour remediation", //
      "vent blockage causing odor – estimated 3-hour fix", //
      "toilet fill valve malfunction – estimated 2-hour replacement", //
      "sink trap blockage – estimated 3-hour clearing", //
      "scrub sink drain slowdown – estimated 4-hour repair", //
      "sewage ejector pump fault – estimated 1-day service", //
      "leak behind wall – estimated 2-day repair"//
  );

  // --- NURSE CALL (10) ---
  List<String> nursCallComments = List.of(//
      "panel glitch – estimated 3-hour rewire", //
      "intermittent bed-side button – estimated 2-hour replacement", //
      "annunciator delay – estimated 4-hour controller reset", //
      "keypad failure – estimated 2-hour fix", //
      "call light stuck active – estimated 3-hour diagnostic", //
      "tone generator fault – estimated 5-hour repair", //
      "network dropouts – estimated 6-hour cable replacement", //
      "wall station power fault – estimated 4-hour service", //
      "wireless pendant failure – estimated 2-hour swap", //
      "zone controller overheating – estimated 1-day replacement"//
  );

  // --- MEDICAL GAS (10) ---
  List<String> medGasComments = List.of(//
      "oxygen pressure fluctuation – estimated 6-hour inspection", //
      "air regulator failure – estimated 4-hour replacement", //
      "oxygen outlet leak – estimated 3-hour repair", //
      "vacuum line restriction – estimated 5-hour clearing", //
      "flowmeter malfunction – estimated 2-hour swap", //
      "negative pressure sensor fault – estimated 5-hour fix", //
      "nitrous oxide valve sticking – estimated 4-hour service", //
      "alarm fault – estimated 3-hour recalibration", //
      "insufflator port failure – estimated 1-day repair", //
      "shutoff valve stiffness – estimated 3-hour lubrication"//
  );

  // --- COMMUNICATIONS / IT (10) ---
  List<String> commsComments = List.of(//
      "cooling fan array failure – estimated 1-day replacement", //
      "CCTV switch overheating – estimated 6-hour swap", //
      "network dropouts – estimated 3-hour cable replacement", //
      "workstation instability – estimated 4-hour rebuild", //
      "Wi-Fi dead zone – estimated 5-hour access point relocation", //
      "label printer network fault – estimated 2-hour fix", //
      "VoIP handset failure – estimated 2-hour replacement", //
      "signage controller crash – estimated 3-hour reload", //
      "monitor network jitter – estimated 6-hour switch tuning", //
      "radio repeater static – estimated 4-hour antenna service"//
  );

  Map<String, List<String>> systemCommentsListMap = Map.of(//
      Hics251Message.SYSTEM_NAMES.get(0), powerComments, //
      Hics251Message.SYSTEM_NAMES.get(1), lightingComments, //
      Hics251Message.SYSTEM_NAMES.get(2), waterComments, //
      Hics251Message.SYSTEM_NAMES.get(3), sewerComments, //
      Hics251Message.SYSTEM_NAMES.get(4), nursCallComments, //
      Hics251Message.SYSTEM_NAMES.get(5), medGasComments, //
      Hics251Message.SYSTEM_NAMES.get(6), commsComments //
  );
}
