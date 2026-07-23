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
import java.util.List;
import java.util.Map;

import com.surftools.utils.BucketChooser;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Hics259Message;
import com.surftools.wimp.message.Hics259Message.CasualtyEntry;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class Hics259Generator extends AbstractBasePracticeGenerator {

  private boolean isInitialized = false;
  private Map<CasualtyType, BucketChooser<String>> casualtyTypeChooserMap;

  @Override
  public void initialize(IConfigurationManager cm) {
    super.initialize(cm);

    if (!isInitialized) {
      casualtyTypeChooserMap = new HashMap<>();
      casualtyTypeChooserMap.put(CasualtyType.ADMITTED, new BucketChooser<String>(patientsAdmitted, rng));
      casualtyTypeChooserMap.put(CasualtyType.BED, new BucketChooser<String>(bedStatus, rng));
      casualtyTypeChooserMap.put(CasualtyType.DISCHARGED, new BucketChooser<String>(patientsDischarged, rng));
      casualtyTypeChooserMap.put(CasualtyType.HOSPITAL_NAMES, data.hospitalNameChooser);
      casualtyTypeChooserMap.put(CasualtyType.SEEN, new BucketChooser<String>(patientsSeen, rng));
      casualtyTypeChooserMap.put(CasualtyType.TRANSFERRED, new BucketChooser<String>(patientsTransferred, rng));
      casualtyTypeChooserMap.put(CasualtyType.WAITING, new BucketChooser<String>(patientsWaiting, rng));
    }
  }

  @Override
  public Hics259Message generateMessage(LocalDate date) {

    var incidentName = "Exercise Id: " + data.getExerciseId();
    var facilityName = data.hospitalNameChooser.next();
    var subject = "HICS-259 HOSPITAL CASUALTY/FATALITY REPORT-" + incidentName;
    var exportedMessage = makeExportedMessage(date, subject);

    var operationalPeriod = String.valueOf(rng.nextInt(1, 3));
    var windowOpenDate = date.minusDays(5);
    var windowCloseDate = date.plusDays(1);

    var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    var opFromDate = dtf.format(windowOpenDate);
    var opFromTime = "00:00";
    var opToDate = dtf.format(windowCloseDate);
    var opToTime = "08:00";

    var casualtyMap = makeCasualtyMap(date);
    var patientTrackingManager = data.doubleNameChooser.next();

    var m = new Hics259Message(exportedMessage, //
        incidentName, NA, NA, //
        operationalPeriod, opFromDate, opFromTime, opToDate, opToTime, //
        casualtyMap, //
        patientTrackingManager, facilityName, NA, NA);

    return m;
  }

  @Override
  public String generateIntructions(ExportedMessage message, LocalDate date) {
    var m = (Hics259Message) message;

    var sb = new StringBuilder(); // exercise instructions
    sb.append("Task: Complete an HICS 259 Hospital Casualty/Fatality Report Message" + NL + NL);
    sb.append(INDENT + "Incident name: " + m.incidentName + NL);
    sb.append(INDENT + "Date: (click in box and accept date)" + NL);
    sb.append(INDENT + "Time: (click in box and accept time)" + NL);
    sb.append(INDENT + "Operational Period #: " + m.operationalPeriod + NL);
    sb.append(INDENT + "Operational Period Date From: " + m.opFromDate + NL);
    sb.append(INDENT + "Operational Period Date To: " + m.opToDate + NL);
    sb.append(INDENT + "Operational Period Time From: " + m.opFromTime + NL);
    sb.append(INDENT + "Operational Period Time To: " + m.opToTime + NL);

    sb.append("Number Of Casualties" + NL);

    for (var key : Hics259Message.CASUALTY_KEYS) {
      var entry = m.casualtyMap.get(key);
      sb.append(INDENT + key + NL);
      sb.append(INDENT2 + "Adult: " + entry.adultCount() + NL);
      sb.append(INDENT2 + "Pediatric: " + entry.childCount() + NL);
      sb.append(INDENT2 + "Comments: " + entry.comment() + NL);
    }
    sb.append("Prepared by: " + m.patientTrackingManager + NL);
    sb.append("Facility Name: " + m.facilityName + NL);

    sb.append(generateInstructionTail());

    return sb.toString();
  }

  public Map<String, CasualtyEntry> makeCasualtyMap(LocalDate date) {
    var map = new HashMap<String, CasualtyEntry>();
    var keys = Hics259Message.CASUALTY_KEYS;
    map.put(keys.get(0), new CasualtyEntry(rng(50, 100), rng(5, 10), get(CasualtyType.SEEN)));
    map.put(keys.get(1), new CasualtyEntry(rng(25, 50), rng(5, 10), get(CasualtyType.WAITING)));
//    map.put(keys.get(2), new CasualtyEntry(rng(20, 60), rng(5, 10), get(CasualtyType.ADMITTED)));
    map.put(keys.get(3), new CasualtyEntry(rng(10, 20), rng(5, 10), get(CasualtyType.BED))); // critical care
    map.put(keys.get(4), new CasualtyEntry(rng(10, 20), rng(5, 10), get(CasualtyType.BED))); // medical/surgical
    map.put(keys.get(5), new CasualtyEntry("0", rng(5, 10), get(CasualtyType.BED))); // pediatric
    map.put(keys.get(6), new CasualtyEntry(rng(10, 20), rng(5, 10), get(CasualtyType.DISCHARGED)));
    map.put(keys.get(7), new CasualtyEntry(rng(10, 20), rng(5, 10), get(CasualtyType.TRANSFERRED)));
    map.put(keys.get(8), new CasualtyEntry("0", "0", "")); // Expired

    /**
     * now, do it right! Admitted *SHOULD BE* the total of critical, medical and
     * pediatric leave the above code, so that we make the same number of calls to
     * the rng
     */
    var criticalEntry = map.get(keys.get(3));
    var medicalEntry = map.get(keys.get(4));
    var pediatricEntry = map.get(keys.get(5));
    var adultAdmitted = stringSum3(criticalEntry.adultCount(), medicalEntry.adultCount(), pediatricEntry.adultCount());
    var pediAdmitted = stringSum3(criticalEntry.childCount(), medicalEntry.childCount(), pediatricEntry.childCount());
    var newAdmittedEntry = new CasualtyEntry(adultAdmitted, pediAdmitted, get(CasualtyType.ADMITTED));
    map.put(keys.get(2), newAdmittedEntry);
    return map;
  }

  private String get(CasualtyType casualtyType) {
    var chooser = casualtyTypeChooserMap.get(casualtyType);
    var value = chooser.next();
    return value;
  }

  enum CasualtyType {
    HOSPITAL_NAMES, SEEN, WAITING, ADMITTED, BED, DISCHARGED, TRANSFERRED
  }

  private List<String> patientsSeen = Arrays.asList("Seen and discharged", "Seen with minor injuries",
      "Seen and admitted to ICU", "Seen and referred to specialist", "Seen with psychological distress",
      "Seen with smoke inhalation", "Seen for dehydration", "Seen for chemical exposure", "Seen for crush injury",
      "Seen for burns - 1st degree", "Seen for burns - 2nd degree", "Seen for fracture assessment",
      "Seen and placed under quarantine", "Seen after delayed arrival", "Seen as walk-in from incident zone",
      "Seen with unknown history", "Seen, identity confirmed", "Seen and refused further care",
      "Seen during secondary triage", "Seen under pediatric protocol");

  private List<String> patientsWaiting = Arrays.asList("Waiting - red tag priority",
      "Waiting - yellow tag, delayed care", "Waiting - green tag, minor injuries",
      "Waiting - black tag confirmation pending", "Waiting - pediatric assessment", "Waiting - geriatric evaluation",
      "Waiting - language interpreter required", "Waiting - behavioral health screening",
      "Waiting - wheelchair assistance", "Waiting - non-verbal communication", "Waiting - injury review in progress",
      "Waiting - vital signs unstable", "Waiting - reassessment post-movement",
      "Waiting - isolated for infection control", "Waiting - exposure to hazardous material",
      "Waiting - identity not confirmed", "Waiting - parental consent required", "Waiting - evacuee priority group",
      "Waiting - re-triage due to condition change", "Waiting - follow-up diagnostics required");

  private List<String> patientsAdmitted = Arrays.asList("Admitted - intensive care unit (ICU)",
      "Admitted - general medical ward", "Admitted - surgical recovery", "Admitted - respiratory support",
      "Admitted - under cardiac monitoring", "Admitted - neurological observation",
      "Admitted - pending imaging results", "Admitted - infectious disease isolation",
      "Admitted - psychiatric evaluation", "Admitted - pediatric care", "Admitted - burn unit treatment",
      "Admitted - trauma stabilization", "Admitted - dehydration protocol",
      "Admitted - chemical exposure decontamination", "Admitted - under quarantine protocol",
      "Admitted - orthopedic management", "Admitted - high-risk pregnancy monitoring", "Admitted - renal support",
      "Admitted - post-operative follow-up", "Admitted - awaiting surgical consult");

  private List<String> bedStatus = Arrays.asList("Beds occupied – trauma patient",
      "Beds available – post-quake inspection cleared", "Beds under quarantine – biohazard exposure",
      "Beds reserved – incoming critical care", "Beds out of service – structural damage",
      "Beds occupied – awaiting transfer to field unit", "Beds ready – emergency sanitation complete",
      "Beds status unknown – communication outage", "Beds available – staffed and functional",
      "Beds occupied – disaster responder admitted", "Beds repurposed – converted to intensive care",
      "Beds inspection pending – infrastructure review", "Beds assigned – overflow emergency triage",
      "Beds offline – power supply limited", "Beds prepared – mobile oxygen unit installed",
      "Beds sealed – infection control protocol", "Beds occupied – casualty stabilization ongoing",
      "Beds requested – regional coordination in progress", "Beds released – patient transfer completed",
      "Beds activated – surge capacity initiated");

  private List<String> patientsDischarged = Arrays.asList("Discharged - recovered fully",
      "Discharged - follow-up required", "Discharged - referred to outpatient care",
      "Discharged - home care instructions provided", "Discharged - non-emergency condition",
      "Discharged - mental health referral", "Discharged - post-surgical recovery",
      "Discharged - relocated to long-term care", "Discharged - stable vitals", "Discharged - after observation period",
      "Discharged - self-transport arranged", "Discharged - caregiver notified", "Discharged - consent obtained",
      "Discharged - telehealth follow-up scheduled", "Discharged - cleared by attending physician",
      "Discharged - wound dressing instructions provided", "Discharged - no treatment required",
      "Discharged - shelter referral given", "Discharged - documentation completed",
      "Discharged - released per triage protocol");

  private List<String> patientsTransferred = Arrays.asList("Transferred - trauma center", "Transferred - burn unit",
      "Transferred - cardiac care facility", "Transferred - pediatric specialty hospital",
      "Transferred - psychiatric treatment center", "Transferred - rehabilitation clinic",
      "Transferred - long-term care facility", "Transferred - infectious disease center",
      "Transferred - orthopedic specialty hospital", "Transferred - surgical suite",
      "Transferred - neurology department", "Transferred - dialysis center", "Transferred - hospice care",
      "Transferred - emergency overflow site", "Transferred - mobile field hospital",
      "Transferred - radiology for diagnostics", "Transferred - isolation unit", "Transferred - maternity care center",
      "Transferred - respiratory support facility", "Transferred - discharge shelter coordination");

  private String rng(int min, int max) {
    return String.valueOf(rng.nextInt(min, max));
  }

  public Map<String, CasualtyEntry> makeCasualtyMap() {
    var map = new HashMap<String, CasualtyEntry>();
    var keys = Hics259Message.CASUALTY_KEYS;
    map.put(keys.get(0), new CasualtyEntry(rng(50, 100), rng(5, 10), get(CasualtyType.SEEN)));
    map.put(keys.get(1), new CasualtyEntry(rng(25, 50), rng(5, 10), get(CasualtyType.WAITING)));
//    map.put(keys.get(2), new CasualtyEntry(rng(20, 60), rng(5, 10), get(CasualtyType.ADMITTED)));
    map.put(keys.get(3), new CasualtyEntry(rng(10, 20), rng(5, 10), get(CasualtyType.BED))); // critical care
    map.put(keys.get(4), new CasualtyEntry(rng(10, 20), rng(5, 10), get(CasualtyType.BED))); // medical/surgical
    map.put(keys.get(5), new CasualtyEntry("0", rng(5, 10), get(CasualtyType.BED))); // pediatric
    map.put(keys.get(6), new CasualtyEntry(rng(10, 20), rng(5, 10), get(CasualtyType.DISCHARGED)));
    map.put(keys.get(7), new CasualtyEntry(rng(10, 20), rng(5, 10), get(CasualtyType.TRANSFERRED)));
    map.put(keys.get(8), new CasualtyEntry("0", "0", "")); // Expired

    /**
     * now, do it right! Admitted *SHOULD BE* the total of critical, medical and
     * pediatric leave the above code, so that we make the same number of calls to
     * the rng
     */
    var criticalEntry = map.get(keys.get(3));
    var medicalEntry = map.get(keys.get(4));
    var pediatricEntry = map.get(keys.get(5));
    var adultAdmitted = stringSum3(criticalEntry.adultCount(), medicalEntry.adultCount(), pediatricEntry.adultCount());
    var pediAdmitted = stringSum3(criticalEntry.childCount(), medicalEntry.childCount(), pediatricEntry.childCount());
    var newAdmittedEntry = new CasualtyEntry(adultAdmitted, pediAdmitted, get(CasualtyType.ADMITTED));
    map.put(keys.get(2), newAdmittedEntry);
    return map;
  }

  /**
   * add three Strings, return a String
   *
   * @param s1
   * @param s2
   * @param s3
   * @return
   */
  private String stringSum3(String s1, String s2, String s3) {
    return String.valueOf(Integer.parseInt(s1) + Integer.parseInt(s2) + Integer.parseInt(s3));
  }

}
