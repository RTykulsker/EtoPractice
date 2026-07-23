/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

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

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.codec.binary.Base32;

import com.surftools.utils.BucketChooser;

public class PracticeData {

  public final BucketChooser<String> roleChooser;
  public final BucketChooser<String> shortRoleChooser;
  public final BucketChooser<String> nameChooser;
  public final BucketChooser<String> doubleNameChooser;
  public final BucketChooser<String> deliveryChooser;
  public final BucketChooser<String> priorityChooser;
  public final BucketChooser<String> hospitalNameChooser;

  public enum ExerciseIdMethod {
    UUID, SHORT_UUID, MID, PHONE, FOUR_BY_FOUR
  }

  private final Random rng;

  public PracticeData(Random rng) {
    this.rng = rng;

    roleChooser = new BucketChooser<String>(emergencyResponseRoles, rng);
    shortRoleChooser = new BucketChooser<String>(shortEmergencyRoles, rng);
    nameChooser = new BucketChooser<String>(names, rng);
    doubleNameChooser = new BucketChooser<String>(doubleNames, rng);
    hospitalNameChooser = new BucketChooser<String>(hospitalNames, rng);
    deliveryChooser = new BucketChooser<String>(deliveryList, rng);
    priorityChooser = new BucketChooser<String>(List.of("Low", "Routine", "URGENT"), rng);
  }

  public String getExerciseId() {
    return getExerciseId(ExerciseIdMethod.PHONE);
  }

  public String getExerciseId(ExerciseIdMethod method) {
    var ret = "";
    switch (method) {
    case UUID:
      ret = java.util.UUID.randomUUID().toString();
    case SHORT_UUID:
      ret = java.util.UUID.randomUUID().toString().substring(9, 23);
    case MID:
      ret = generateMid(java.util.UUID.randomUUID().toString());
    case PHONE:
      var n = (long) (rng.nextDouble() * 9000000000L) + 1000000000L;
      var s = String.valueOf(n);
      ret = s.substring(0, 3) + "-" + s.substring(3, 6) + "-" + s.substring(6);
    case FOUR_BY_FOUR:
      n = (long) (rng.nextDouble() * 9000000000L) + 1000000000L;
      s = String.valueOf(n);
      ret = s.substring(0, 4) + "-" + s.substring(4, 8);
    }

    return ret;
  }

  private String generateMid(String string) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      String stringToHash = string + System.nanoTime();
      md.update(stringToHash.getBytes());
      byte[] digest = md.digest();
      Base32 base32 = new Base32();
      String encodedString = base32.encodeToString(digest);
      String subString = encodedString.substring(0, 12);
      return subString;
    } catch (Exception e) {
      throw new RuntimeException("could not generate messageId: " + e.getMessage());
    }
  }

  private static final List<String> emergencyResponseRoles = Arrays.asList("Director of Emergency Management",
      "Deputy Director of Operations", "Chief of Staff", "Emergency Response Coordinator", "Strategic Planning Officer",
      "Crisis Management Director", "Regional Response Commander", "Assistant Director of Field Operations",
      "Chief Resilience Officer", "Director of Interagency Coordination", "Incident Commander",
      "Operations Section Chief", "Field Response Supervisor", "Emergency Services Officer",
      "Search and Rescue Team Leader", "Disaster Response Specialist", "Urban Search and Rescue Technician",
      "Hazardous Materials (HAZMAT) Officer", "Emergency Operations Center (EOC) Manager", "Rapid Response Team Member",
      "Planning Section Chief", "Situation Analyst", "Intelligence Officer", "Risk Assessment Specialist",
      "Continuity of Operations Planner", "Emergency Preparedness Analyst",
      "Geographic Information Systems (GIS) Analyst", "Threat Assessment Coordinator", "Scenario Planning Officer",
      "Data Modeling Specialist", "Logistics Section Chief", "Supply Chain Manager", "Resource Allocation Officer",
      "Transportation Coordinator", "Equipment Inventory Specialist", "Fleet Operations Manager",
      "Warehouse Supervisor", "Procurement Officer", "Fuel and Energy Logistics Officer",
      "Shelter Logistics Coordinator", "Chief Medical Officer", "Emergency Medical Services (EMS) Coordinator",
      "Public Health Liaison", "Epidemiologist", "Medical Triage Officer", "Mass Casualty Incident Coordinator",
      "Mental Health Response Specialist", "Medical Logistics Officer", "Field Medic", "Biohazard Response Technician",
      "Public Information Officer", "Communications Section Chief", "Media Relations Specialist",
      "Crisis Communications Strategist", "Social Media Response Coordinator", "Emergency Alert System Manager",
      "Community Outreach Officer", "Multilingual Communications Specialist", "Call Center Supervisor",
      "Internal Communications Manager", "IT Systems Administrator", "Emergency Communications Technician",
      "Cybersecurity Analyst", "Radio Communications Officer", "Infrastructure Resilience Engineer",
      "Power Systems Specialist", "Emergency Technology Integration Officer", "Drone Operations Coordinator",
      "Mobile Command Unit Technician", "Network Operations Center (NOC) Analyst", "Emergency Law Advisor",
      "Regulatory Compliance Officer", "Policy Development Analyst", "Intergovernmental Affairs Liaison",
      "Legal Risk Assessor", "Grants and Funding Specialist", "Mutual Aid Agreements Coordinator",
      "Ethics and Standards Officer", "Legislative Affairs Officer", "Public Safety Policy Analyst",
      "Training and Exercises Coordinator", "Emergency Response Instructor", "Simulation Designer",
      "Certification Program Manager", "Field Training Officer", "Volunteer Training Specialist",
      "Curriculum Development Specialist", "Community Preparedness Educator", "Safety Drill Coordinator",
      "Knowledge Management Officer", "Administrative Services Manager", "Budget and Finance Officer",
      "Human Resources Liaison", "Records and Documentation Specialist", "Grant Writer", "Scheduling Coordinator",
      "Facilities Manager", "Travel and Deployment Coordinator", "Timekeeping and Payroll Officer",
      "Volunteer Program Manager");

  private static final List<String> shortEmergencyRoles = Arrays.asList("EM Director", "Ops Chief", "Chief of Staff",
      "Rescue Leader", "Field Officer", "HAZMAT Officer", "EOC Manager", "Logistics Chief", "Medical Officer",
      "Public Info Lead", "Search Leader", "Rescue Tech", "Disaster Lead", "EMS Coordinator", "Crisis Planner",
      "Risk Analyst", "GIS Analyst", "Intel Officer", "Ops Planner", "Supply Officer", "Fleet Manager",
      "Warehouse Lead", "Procurement Lead", "Shelter Lead", "Triage Officer", "Epidemiologist", "Mental Health",
      "Biohazard Tech", "Comms Officer", "Media Liaison", "Alert Manager", "Outreach Lead", "Call Center Lead",
      "IT Admin", "Radio Tech", "Cyber Analyst", "Infra Engineer", "Drone Operator", "Mobile Tech", "NOC Analyst",
      "Legal Advisor", "Policy Analyst", "Compliance Lead", "Liaison Officer", "Grants Officer", "Ethics Officer",
      "Legislative Lead", "Safety Analyst", "Trainer", "Drill Planner", "Instructor", "Cert Manager", "Field Trainer",
      "Volunteer Lead", "Educator", "Drill Leader", "Knowledge Lead", "Admin Manager", "Finance Officer", "HR Liaison",
      "Records Clerk", "Grant Writer", "Scheduler", "Facilities Lead", "Travel Planner", "Payroll Officer",
      "Volunteer Coord", "Ops Director", "Deputy Director", "Resilience Lead", "Interagency Lead", "Rapid Responder",
      "SAR Tech", "HAZMAT Tech", "Continuity Lead", "Scenario Lead", "Data Modeler", "Transport Lead", "Inventory Lead",
      "Fuel Officer", "Medical Tech", "Mass Casualty", "Comms Strategist", "Social Media", "Internal Comms",
      "Tech Integrator", "Power Specialist", "Radio Officer", "Legal Risk", "Mutual Aid Lead", "Ethics Lead",
      "Affairs Officer", "Policy Officer", "Drill Coord", "Safety Officer", "Curriculum Lead", "Prep Educator",
      "Admin Officer", "Budget Officer", "HR Officer", "Doc Specialist", "Travel Officer", "Timekeeper");

  private static final List<String> names = Arrays.asList("Liam Carter", "Emma Brooks", "Noah Hayes", "Olivia Grant",
      "Elijah Stone", "Ava Morgan", "James Reed", "Sophia Lane", "Benjamin Fox", "Isabella Ross", "Lucas Ward",
      "Mia Blake", "Henry Wells", "Amelia Dean", "Alexander Cole", "Harper West", "William Nash", "Evelyn Shaw",
      "Daniel Boyd", "Abigail Ray", "Matthew King", "Ella Ford", "Jackson Hale", "Scarlett Moon", "Sebastian Lee",
      "Grace Hunt", "David Knox", "Chloe Page", "Joseph Tate", "Lily Snow", "Samuel Webb", "Zoey Hart", "Owen Cross",
      "Nora Quinn", "Wyatt Long", "Aria Bell", "John York", "Layla Park", "Julian Ross", "Riley Nash", "Levi Dean",
      "Ellie Wood", "Isaac Lane", "Luna Frost", "Gabriel West", "Hazel Knox", "Anthony Cole", "Violet Ray",
      "Andrew Hale", "Aurora King", "Lincoln Boyd", "Penelope Shaw", "Christopher Fox", "Camila Wells", "Joshua Stone",
      "Stella Moon", "Nathan Ward", "Paisley Hunt", "Caleb Nash", "Savannah Page", "Eli Ford", "Brooklyn Snow",
      "Thomas Webb", "Claire Hart", "Aaron Cross", "Skylar Quinn", "Charles Long", "Lucy Bell", "Hunter York",
      "Anna Park", "Adrian Ross", "Caroline Nash", "Jonathan Dean", "Madeline Wood", "Christian Lane", "Elena Frost",
      "Connor West", "Naomi Knox", "Jeremiah Cole", "Ruby Ray", "Robert Hale", "Ivy King", "Easton Boyd",
      "Kinsley Shaw", "Jordan Fox", "Aaliyah Wells", "Angel Stone", "Cora Moon", "Dominic Ward", "Sadie Hunt",
      "Austin Nash", "Julia Page", "Brayden Ford", "Piper Snow", "Jason Webb", "Eva Hart", "Miles Cross", "Alice Quinn",
      "Xavier Long", "Faith Bell", "Justin York", "Maya Park");

  private static final List<String> doubleNames = Arrays.asList("Alice Anderson", "Ben Baxter", "Catherine Carter",
      "Daniel Dawson", "Ella Edwards", "Franklin Foster", "Grace Griffin", "Henry Hughes", "Isla Ingram",
      "Jack Johnson", "Kara Keller", "Liam Lawson", "Mia Mitchell", "Noah Nelson", "Olivia Owens", "Peter Parker",
      "Quinn Quigley", "Rachel Rivers", "Samuel Scott", "Tara Thompson", "Ulysses Underwood", "Violet Vaughn",
      "William Walker", "Xander Xavier", "Yara Young", "Zane Zimmerman", "Amber Allen", "Brandon Brooks",
      "Chloe Chambers", "Dylan Drake", "Eva Ellison", "Felix Fisher", "Georgia Grant", "Hannah Hall", "Ian Irving",
      "Julia Jenkins", "Kyle Knight", "Leo Larson", "Mason Moore", "Natalie Neal", "Owen Ortega", "Paige Preston",
      "Quentin Quinn", "Rebecca Ross", "Sean Simmons", "Tina Taylor", "Uriel Upton", "Vanessa Vance", "Wesley West",
      "Ximena Xanders", "Yosef Yates", "Zara Ziegler", "Aiden Avery", "Bella Benson", "Caleb Cooper", "Daisy Dalton",
      "Ethan Ellis", "Fiona Flynn", "Gavin Gates", "Hailey Harmon", "Isaac Iverson", "Jasmine James", "Kevin Kirk",
      "Lily Lewis", "Madeline Marks", "Nathan Norris", "Olive O’Connor", "Peyton Pierce", "Quincy Quick",
      "Riley Randall", "Sophie Sanders", "Trevor Tate", "Uma Underhill", "Victor Vega", "Wendy Winters", "Xenia Xiong",
      "Yvonne York", "Zachary Zane", "Annie Abbott", "Blake Barnes", "Clara Clayton", "Derek Dorsey", "Eliza Emerson",
      "Finn Franklin", "Gemma Garrison", "Harper Hines", "Ivan Ives", "Jared Jennings", "Kylie King", "Logan Lane",
      "Megan Murphy", "Nolan Nash", "Oscar Olsen", "Phoebe Phelps", "Quora Quinlan", "Ronald Reid", "Samantha Steele",
      "Tristan Turner", "Ulric Urban", "Valerie Vaughn", "Willa Wade", "Xavier Xenos", "Yahir Yoder", "Zoey Zane");

  // MUST BE 30 characters or less
  private List<String> hospitalNames = Arrays.asList(//
      "Mercy Ridge Medical Center", //
      "Summit Peak General Hospital", //
      "Evergreen Regional Health", //
      "Crescent Valley Trauma Center", //
      "Starlight Children's Hospital", //
      "Horizon Behavioral Health", //
      "Red Rock Emergency Hospital", //
      "Blue River Community Medical", //
      "Golden Gate Cardiac Institute", //
      "Willow Grove Hospital", //
      "Northbridge Memorial Hospital", //
      "Cascade Lake Surgical Hospital", //
      "Silver Lake Medical Facility", //
      "Twin Pines Memorial Hospital", //
      "Ironwood Memorial Hospital", //
      "Liberty Field Mobile Hospital", //
      "Maplecrest Veterans Hospital", //
      "Oceanview Regional Hospital", //
      "Prairie Hill Long-Term Care", //
      "Lakeshore Medical and Imaging");

  List<String> deliveryList = Arrays.asList("Main Entrance", "Reception Desk", "Loading Dock", "Mailroom",
      "Security Office", "Parking Lot – North", "Parking Lot – South", "Visitor Entrance", "Side Entrance",
      "Rooftop Access", "Maintenance Room", "Basement Level", "Elevator Lobby – 1st Floor",
      "Elevator Lobby – 10th Floor", "Conference Room A", "Cafeteria Entrance", "Bike Rack Area",
      "Fire Exit – East Wing", "Drop Box – West Gate", "Garden Courtyard");

}
