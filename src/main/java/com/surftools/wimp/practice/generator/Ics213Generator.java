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
import java.util.List;

import com.surftools.utils.BucketChooser;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.practice.generator.PracticeData.ExerciseIdMethod;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class Ics213Generator extends AbstractBasePracticeGenerator {
  private BucketChooser<String> messageChooser;

  @Override
  public void initialize(IConfigurationManager cm) {
    super.initialize(cm);
    messageChooser = new BucketChooser<String>(messageList, rng);
  }

  @Override
  public Ics213Message generateMessage(LocalDate date) {
    var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    var formSubject = "ETO Practice Exercise for " + dtf.format(date);
    var subject = "ICS-213: " + formSubject;
    var exportedMessage = makeExportedMessage(date, subject);

    var organization = "EmComm Training Organization";
    var incidentName = "Exercise Id: " + data.getExerciseId(ExerciseIdMethod.PHONE);
    var formFrom = data.nameChooser.next() + " / " + data.roleChooser.next();
    var formTo = data.nameChooser.next() + " / " + data.roleChooser.next();

    var formDate = NA;
    var formTime = NA;
    var formMessage = messageChooser.next();
    var approvedBy = data.nameChooser.next();
    var position = data.roleChooser.next();
    var isExercise = true;
    var formLocation = LatLongPair.ZERO_ZERO;
    var version = NA;
    var expressVersion = NA;
    var dataSource = NA;

    var m = new Ics213Message(exportedMessage, organization, incidentName, //
        formFrom, formTo, formSubject, formDate, formTime, //
        formMessage, approvedBy, position, //
        isExercise, formLocation, version, expressVersion, dataSource);

    return m;
  }

  @Override
  public String generateIntructions(ExportedMessage message, LocalDate date) {
    var m = (Ics213Message) message;

    var sb = new StringBuilder();
    sb.append(generateInstructionHeader(date, "Complete an ICS-213 General Message"));
    sb.append(INDENT + "Setup: agency or group name: " + m.organization + NL);
    sb.append(INDENT + "THIS IS AN EXERCISE: (checked)" + NL);
    sb.append(INDENT + "Incident Name: " + m.incidentName + NL);
    sb.append(INDENT + "To (Name/Position): " + m.formTo + NL);
    sb.append(INDENT + "From (Name/Position): " + m.formFrom + NL);
    sb.append(INDENT + "Subject: " + m.formSubject + NL);
    sb.append(INDENT + "Date: (click in box and accept date)" + NL);
    sb.append(INDENT + "Time: (click in box and accept time)" + NL);
    sb.append(INDENT + "Message: " + m.formMessage + NL);
    sb.append(INDENT + "Approved by: " + m.approvedBy + NL);
    sb.append(INDENT + "Position / Title: " + m.position + NL);
    sb.append(generateInstructionTail());

    return sb.toString();
  }

  private static final List<String> messageList = Arrays.asList(
      "Air quality moderate; smoke levels rising near fire line.", "Backup generator failed; technician dispatched.",
      "Bridge closed due to ice accumulation; detour established.",
      "Bridge inspection complete; minor structural cracking observed.", "Bridge inspection complete; safe for travel.",
      "Bridge inspection confirms structural integrity.", "Bridge inspection reveals loose railing; repair ordered.",
      "Bridge inspection reveals no critical damage.", "Bridge inspection shows minor erosion; no closure needed.",
      "Bridge inspection shows no hazards.", "Bridge reopened after de-icing; traffic normal.",
      "Cell service intermittent; switching to radio-only comms.",
      "Communications stable on VHF; UHF experiencing interference.",
      "Cooling center open; attendance increasing due to heat.", "Cooling center operating smoothly.",
      "Cooling center receiving donated supplies.", "Cooling center receiving steady flow of residents.",
      "Cooling center requesting additional ice shipments.", "Cooling center requesting additional volunteers.",
      "Cooling center requesting more water.", "Crews clearing mudslide debris; estimated completion 2 hours.",
      "Crews clearing mudslide debris; estimated completion two hours.",
      "Crews reinforcing fire break ahead of wind shift.", "Crews reinforcing levee; no breaches reported.",
      "Crews repairing downed power lines near substation.", "Crews restoring power to hospital backup systems.",
      "Dam inspection complete; no structural compromise detected.",
      "EMS responding to vehicle rollover; icy conditions noted.", "EMS transporting burn patient; condition moderate.",
      "EMS transporting cardiac patient; priority high.", "EMS transporting elderly patient; stable.",
      "EMS transporting trauma patient; ETA ten minutes.", "EMS transporting two patients to regional medical center.",
      "EMS treating chest pain; transport required.", "EMS treating fall injury; patient stable.",
      "EMS treating hypothermia patient; transport imminent.", "EMS treating minor fall injury.",
      "EMS treating minor injuries from debris impact.", "EMS treating minor lacerations; patient stable.",
      "EMS unit requesting additional bandages and splints.",
      "Evac center reports 12 new arrivals; processing underway.", "Evac center requesting additional bottled water.",
      "Evac center requesting additional hygiene supplies.",
      "Evac team assisting elderly resident with medication needs.",
      "Evac team assisting mobility-impaired residents in Zone 2.", "Evac team reports Zone 1 fully cleared.",
      "Evac team reports Zone 4 cleared; no stragglers.", "Evac team reports all pets relocated from shelter zone.",
      "Evac team reports all residents accounted for.", "Evac team reports all residents cleared from Zone 3.",
      "Evac team reports all shelters supplied.", "Evac team reports full compliance.",
      "Evacuation buses staged and ready for deployment.", "Evacuation of campground complete; area secured.",
      "Evacuation route clear; traffic moving steadily.", "Fire containment at 100 percent; incident controlled.",
      "Fire containment at 40 percent; progress steady.", "Fire containment at 55 percent; progress improving.",
      "Fire containment at 62 percent; crews optimistic.", "Fire containment at 70 percent; progress steady.",
      "Fire containment at 78 percent; strong progress.", "Fire containment at 82 percent; nearing control.",
      "Fire containment at 90 percent; strong control.", "Fire containment at 95 percent; near completion.",
      "Fire crew demobilizing equipment.", "Fire crew extinguished final hotspot.",
      "Fire crew extinguished flare-up near containment edge.", "Fire crew extinguished hotspot near containment line.",
      "Fire crew extinguished smoldering debris pile.",
      "Fire crew reports flare-up on west flank; suppression ongoing.",
      "Fire crew requesting additional hose line support.", "Fire crew rotating due to heat stress.",
      "Fire crew rotating personnel due to heat exposure.", "Fire crew rotating shifts due to fatigue.",
      "Fire line breached; crews repositioning to contain spread.", "Fire line expanded to protect nearby structures.",
      "Fire line holding despite wind shift; monitoring continues.", "Fire line holding overnight; no flare-ups.",
      "Fire line reinforced on east flank; conditions stable.", "Fire line reinforced with additional crews.",
      "Fire line secure; no active flames.", "Fire line strengthened with bulldozer support.",
      "Fire retardant drop completed; effectiveness high.",
      "Fire spotting detected beyond ridge; recon team dispatched.",
      "Fire spotting observed; aerial support requested.", "Flood barrier holding; monitoring seepage at south end.",
      "Flood barrier reinforced; seepage reduced.", "Flood debris blocking alleyway; clearing underway.",
      "Flood debris cleared from culvert.", "Flood debris cleared from parking lot.",
      "Flood debris clogging drainage; clearing underway.", "Flood debris removed from culvert; flow restored.",
      "Flood debris removed from roadway.", "Flood debris removed from storm drain.",
      "Flood gauge rising faster than expected; alert issued.", "Flood watch extended; river level rising slowly.",
      "Floodwater contamination suspected; samples collected.", "Floodwaters low; no threat.",
      "Floodwaters receding; cleanup beginning.", "Floodwaters receding; damage assessment underway.",
      "Floodwaters rising slowly; monitoring hourly.", "Floodwaters stable; monitoring continues.",
      "Floodwaters stable; no new evacuations required.", "Floodwaters stagnant; mosquito risk increasing.",
      "Generator operational; fuel supply at 60 percent.", "Hazmat team staged and ready for railcar inspection.",
      "Landslide debris blocking northbound lane; no injuries reported.",
      "Medical supply cache inventoried; levels sufficient.", "Medical team requesting additional PPE supplies.",
      "Medical team requesting additional gloves.", "Medical team requesting additional saline bags.",
      "Medical team treating allergic reaction.", "Medical team treating allergic reaction; EpiPen administered.",
      "Medical team treating diabetic emergency; stable.", "Medical team treating sprain.",
      "Medical team treating sprained ankle; no transport needed.",
      "Medical triage area set up; awaiting incoming evacuees.", "Medical unit reports stable patient conditions.",
      "Medical unit treating asthma exacerbation due to smoke.",
      "Medical unit treating dehydration cases; supplies adequate.", "Medical unit treating dehydration.",
      "Medical unit treating heat exhaustion; cooling measures applied.", "Medical unit treating minor burns.",
      "Medical unit treating multiple minor injuries.", "Medical unit treating smoke irritation; mild.",
      "Medical unit treating three patients for smoke inhalation.", "Power grid stable after minor adjustments.",
      "Power grid stable and balanced.", "Power grid stable; no further issues.",
      "Power grid stable; no further outages expected.", "Power outage affecting approximately 120 homes in Sector A.",
      "Power outage isolated to two blocks; repair underway.", "Power restored to community center.",
      "Power restored to fire station; operations normal.", "Power restored to municipal building; systems normal.",
      "Power restored to residential block; grid stable.", "Radio repeater functioning; coverage stable across valley.",
      "Road crew repairing potholes caused by storm runoff.", "Road graders deployed to clear ice from main highway.",
      "Roadway blocked by fallen trees; crews en route.", "Roadway partially flooded; cones placed for safety.",
      "Roadway reopened after debris removal; safe for travel.",
      "Roadway reopened after snow clearance; traction good.", "Search dogs deployed in rubble zone; results pending.",
      "Search team clearing attic space; no victims found.", "Search team clearing basement area; no victims found.",
      "Search team clearing final room.", "Search team clearing final sector; no victims found.",
      "Search team completing sweep.", "Search team entering collapsed structure for secondary sweep.",
      "Search team found blocked stairwell; clearing debris.",
      "Search team located missing hiker; extraction underway.", "Search team marking compromised stairwell.",
      "Search team marking hazard-free areas.", "Search team marking hazardous areas for follow-up.",
      "Search team marking safe entry points for crews.", "Search team marking safe zones.",
      "Search team marking unstable flooring.", "Search team reports missing person possibly relocated.",
      "Search team reports unstable debris; marking hazard zone.", "Search team using drones for aerial assessment.",
      "Search team using drones for mapping.", "Search team using thermal imaging; no heat signatures found.",
      "Shelter at full capacity; overflow site activated.",
      "Shelter capacity at 85 percent; requesting additional cots.",
      "Shelter capacity at 85%; requesting additional cots.",
      "Shelter meal service operating normally; supplies sufficient.",
      "Shelter receiving donated supplies; inventory in progress.",
      "Shelter requesting additional blankets due to cold conditions.",
      "Snow accumulation heavy; roof collapse risk increasing.", "Snow accumulation heavy; warming center activated.",
      "Snow accumulation manageable; plows effective.", "Snow accumulation slowing traffic; plows redeployed.",
      "Snow accumulation tapering off; roads improving.", "Snow load causing sagging roof; engineer requested.",
      "Snow load heavy on roofs; inspection teams activated.", "Snowplow mechanical issue; replacement unit en route.",
      "Storm causing intermittent power flickers; monitoring grid.", "Storm causing low visibility; operations slowed.",
      "Storm causing sporadic hail; vehicles advised to shelter.",
      "Storm debris causing minor traffic delays; no injuries.", "Storm debris cleared from school entrance.",
      "Storm debris cleared from sidewalks.", "Storm debris damaging signage; replacements requested.",
      "Storm debris minimal; roads clear.", "Storm debris removed from main intersection.",
      "Storm front moving east; rainfall decreasing.", "Storm shelter HVAC malfunctioning; repair requested.",
      "Storm shelter generator overheating; cooldown initiated.", "Storm surge decreasing; coastal roads reopening.",
      "Storm surge decreasing; docks reopening.", "Storm surge minimal; beach access reopened.",
      "Storm surge minimal; coastal operations normal.", "Storm surge negligible; all clear.",
      "Storm surge overtopping seawall; monitoring for escalation.",
      "Storm surge receding; harbor operations resuming.", "Storm winds damaging temporary fencing; repairs underway.",
      "Team reports chemical odor dissipating; area ventilated.", "Team reports comms interference decreasing.",
      "Team reports comms stable across all channels.", "Team reports comms strong.",
      "Team reports comms tower fully operational.", "Team reports comms tower stable; no structural issues.",
      "Team reports comms tower vibration reduced.", "Team reports gas leak contained; monitoring continues.",
      "Team reports gas leak fully resolved.", "Team reports gas leak repaired; area reopened.",
      "Team reports minor aftershocks; no new damage.", "Team reports minor landslide near trailhead; no injuries.",
      "Team reports minor structural damage to school building.", "Team reports propane hazard eliminated.",
      "Team reports propane leak resolved; area safe.", "Team reports propane tank relocated; hazard cleared.",
      "Team reports propane tank secured with straps.", "Team reports propane tank secured; hazard mitigated.",
      "Traffic control established at intersection; no delays.", "Traffic diverted around washed-out shoulder.",
      "Traffic diverted due to downed power pole.", "Traffic diverted due to sinkhole; repair crew notified.",
      "Traffic normal after debris removal.", "Traffic rerouted due to downed tree; removal in progress.",
      "Traffic rerouted due to icy bridge deck.", "Traffic rerouted due to water pooling.",
      "Tree removal crew delayed; alternate team dispatched.", "Tsunami sirens tested and functioning normally.",
      "Urban search team marking completed structures.", "Water distribution normal; tanks full.",
      "Water distribution point active; supplies adequate.", "Water distribution resumed after pump repair.",
      "Water distribution slowed due to pump issue; repair ongoing.",
      "Water distribution slowed due to staffing shortage.", "Water distribution steady; supplies adequate.",
      "Water distribution uninterrupted.", "Water main rupture near 5th Street; pressure dropping.",
      "Water rescue team deployed; searching flooded intersection.",
      "Water rescue team monitoring rising creek levels.", "Water rescue team recovered stranded vehicle occupants.",
      "Water supply truck delayed; alternate route selected.", "Water treatment plant operating on backup power.",
      "Water treatment pumps running at reduced capacity.", "Wildland fire perimeter holding; no forward progress.",
      "Wildland fire spotting near ridge; crews repositioning.", "Wind damage limited to signage.",
      "Wind damage minor; no structural concerns.", "Wind damage repaired quickly.",
      "Wind damage to awnings reported; repairs scheduled.", "Wind damage to roof reported; contractor notified.",
      "Wind damage to trees significant; removal crews deployed.",
      "Wind gusts exceeding 50 mph; operations paused temporarily.",
      "Winter storm causing drifting snow; plows deployed.");
}
