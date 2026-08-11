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
import java.util.Arrays;
import java.util.List;

import com.surftools.utils.BucketChooser;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.message.BloodAvailabilityMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.schedule.ScheduleRecord;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class BloodAvailabilityGenerator extends AbstractBasePracticeGenerator {

  private BucketChooser<String> faciltyNameChooser;
  private BucketChooser<String> facilityAddressChooser;
  private BucketChooser<Double> multiplierChooser;
  private BucketChooser<Double> zeroChooser;
  private BucketChooser<LatLongPair> pairChooser;

  @Override
  public void initialize(IConfigurationManager cm) {
    super.initialize(cm);

    faciltyNameChooser = new BucketChooser<String>(PracticeData.hospitalNames, baseRng);
    facilityAddressChooser = new BucketChooser<String>(streetNames, baseRng);
    multiplierChooser = new BucketChooser<Double>(List.of(1.0, 0.9, 1.1, .95, 1.05), baseRng);
    zeroChooser = new BucketChooser<Double>(List.of(1d, 1d, 1d, 0d), baseRng);
    pairChooser = new BucketChooser<LatLongPair>(latlongs, baseRng);
  }

  @Override
  public BloodAvailabilityMessage generateMessage(LocalDate date, ScheduleRecord schedule) {
    var facilityName = faciltyNameChooser.next();
    var subject = "Blood Availability: " + facilityName;
    var exportedMessage = makeExportedMessage(date, subject);

    var isExercise = true;
    var formDateTime = NA;

    var dateRng = getRandom(date.toString());

    var facilityAddress = String.valueOf(dateRng.nextInt(1000, 10_000)) + " " + facilityAddressChooser.next();
    var facilityContactName = data.nameChooser.next();
    var facilityPhoneNumber = data.getPhoneNumber();

    var multiplier = multiplierChooser.next();
    var zeroOrOne = zeroChooser.next();
    var redOPlus = compute(38, multiplier, null);
    var redOMinus = compute(7, multiplier, null);
    var redAPlus = compute(34, multiplier, null);
    var redAMinus = compute(6, multiplier, null);
    var redBPlus = compute(9, multiplier, null);
    var redBMinus = compute(2, multiplier, zeroOrOne);
    var redABPlus = compute(3, multiplier, zeroOrOne);
    var redABMinus = compute(1, multiplier, zeroOrOne);

    var plasmaO = compute(45, multiplier, null);
    var plasmaA = compute(40, multiplier, null);
    var plasmaB = compute(11, multiplier, null);
    var plasmaAB = compute(4, multiplier, zeroOrOne);
    var comments = data.getExerciseId(date);
    var approvedBy = data.doubleNameChooser.next();
    var addAttachment = "No";

    var pair = pairChooser.next();
    var formLatitude = pair.getLatitude();
    var formLongitude = pair.getLongitude();
    var version = NA;
    var expressVersion = NA;

    var m = new BloodAvailabilityMessage(exportedMessage, isExercise, formDateTime, //
        facilityName, facilityAddress, facilityContactName, facilityPhoneNumber, //
        redOPlus, redOMinus, redAPlus, redAMinus, redBPlus, redBMinus, redABPlus, redABMinus, //
        plasmaO, plasmaA, plasmaB, plasmaAB, //
        comments, approvedBy, addAttachment, formLatitude, formLongitude, version, expressVersion);

    return m;
  }

  private String compute(int base, double multiplier, Double chooserValuePresent) {
    double chooserValue = chooserValuePresent != null ? chooserValuePresent : 1;
    Double doubleValue = base * multiplier * chooserValue;
    int intValue = doubleValue.intValue();
    return String.valueOf(intValue);
  }

  @Override
  public String generateIntructions(ExportedMessage message, LocalDate date, ScheduleRecord schedule) {
    var m = (BloodAvailabilityMessage) message;

    var sb = new StringBuilder();
    sb.append(generateInstructionHeader(date, "Complete a General Medical Forms/Blood Availablity Message"));
    sb.append(INDENT + "THIS IS AN EXERCISE: (checked)" + NL);
    sb.append(INDENT + "Date/Time: (click in box and accept date/time)" + NL);
    sb.append(INDENT + "Facility Name: " + m.facilityName + NL);
    sb.append(INDENT + "Facility Address: " + m.facilityAddress + NL);
    sb.append(INDENT + "Facility Contact Name: " + m.facilityContactName + NL);
    sb.append(INDENT + "Facility Phone Number: " + m.facilityPhoneNumber + NL);
    sb.append(NL);
    sb.append(INDENT + "RED BLOOD CELLS" + NL);
    sb.append(INDENT2 + "O+: " + m.redOPlus + NL);
    sb.append(INDENT2 + "O-: " + m.redOMinus + NL);
    sb.append(INDENT2 + "A+: " + m.redAPlus + NL);
    sb.append(INDENT2 + "A-: " + m.redAMinus + NL);
    sb.append(INDENT2 + "B+: " + m.redBPlus + NL);
    sb.append(INDENT2 + "B-: " + m.redBMinus + NL);
    sb.append(INDENT2 + "AB+: " + m.redABPlus + NL);
    sb.append(INDENT2 + "AB-: " + m.redABMinus + NL);
    sb.append(NL);
    sb.append(INDENT + "PLASMA" + NL);
    sb.append(INDENT2 + "O: " + m.plasmaO + NL);
    sb.append(INDENT2 + "A: " + m.plasmaA + NL);
    sb.append(INDENT2 + "B: " + m.plasmaB + NL);
    sb.append(INDENT2 + "AB: " + m.plasmaAB + NL);
    sb.append(NL);
    sb.append(INDENT + "Comment: " + m.comments + NL);
    sb.append(INDENT + "Approved by: " + m.approvedBy + NL);
    sb.append(NL);
    sb.append(INDENT + "Form Location LATITUDE: " + m.formLatitude + NL);
    sb.append(INDENT + "Form Location LONGITUDE: " + m.formLongitude + NL);
    sb.append(generateInstructionTail());

    return sb.toString();
  }

  private List<String> streetNames = Arrays.asList("Maple Street", "Oak Avenue", "Pine Street", "Cedar Lane",
      "Birch Road", "Elm Street", "Willow Drive", "Spruce Avenue", "Chestnut Street", "Walnut Lane", "Cherry Street",
      "Ash Avenue", "Poplar Drive", "Hawthorne Street", "Magnolia Road", "Sycamore Lane", "Juniper Street",
      "Alder Avenue", "Fir Street", "Hemlock Drive", "River Road", "Lakeview Drive", "Hillcrest Avenue",
      "Sunset Boulevard", "Highland Drive", "Valley Road", "Forest Avenue", "Meadow Lane", "Ridge Street",
      "Parkview Drive", "Broadway", "Main Street", "Center Street", "Union Avenue", "Washington Street",
      "Jefferson Avenue", "Adams Street", "Madison Street", "Franklin Avenue", "Lincoln Street", "Roosevelt Avenue",
      "Jackson Street", "Monroe Street", "Grant Avenue", "Hamilton Street", "Liberty Road", "Independence Avenue",
      "Front Street", "Market Street", "Railroad Avenue", "Harbor Drive", "Bay Street", "Shoreline Road",
      "Seaview Avenue", "Ocean Boulevard", "Coastline Drive", "Beacon Street", "Lighthouse Road", "Terrace Avenue",
      "Overlook Drive", "Summit Street", "Mountain View Road", "Alpine Avenue", "Glacier Street", "Prairie Road",
      "Timber Lane", "Creekside Drive", "Riverside Avenue", "Brook Street", "Streamside Road", "Canyon Drive",
      "Cliffside Avenue", "Bluff Road", "Stonebridge Drive", "Bridge Street", "Mill Road", "Foundry Street",
      "Factory Avenue", "Depot Street", "Station Road", "College Avenue", "Campus Drive", "Academy Street",
      "Library Road", "Heritage Lane", "Prospect Street", "Maplewood Drive", "Greenwood Avenue", "Fairview Street",
      "Pleasant Hill Road", "Country Lane", "Garden Street", "Orchard Avenue", "Vine Street", "Harvest Road",
      "Sunrise Drive", "Windmill Lane", "Old Town Road", "Riverbend Drive");

  private List<LatLongPair> latlongs = Arrays.asList(new LatLongPair(38.0228, -107.6714),
      new LatLongPair(38.4783, -107.8762), new LatLongPair(38.5458, -106.9253), new LatLongPair(37.2753, -107.8801),
      new LatLongPair(39.0639, -108.5506), new LatLongPair(38.0875, -102.6205), new LatLongPair(39.0328, -104.4741),
      new LatLongPair(39.2508, -106.2925), new LatLongPair(39.2633, -103.6922), new LatLongPair(39.3061, -102.2696),
      new LatLongPair(39.5505, -107.3248), new LatLongPair(37.6242, -104.7833), new LatLongPair(37.9853, -103.5438),
      new LatLongPair(40.2503, -103.7991), new LatLongPair(40.4842, -106.8317), new LatLongPair(40.6255, -103.2077));
}
