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

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.persistence.IPersistenceManager;
import com.surftools.wimp.persistence.PersistenceManager;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.JoinedUser;
import com.surftools.wimp.persistence.dto.ReturnStatus;
import com.surftools.wimp.utils.config.IConfigurationManager;
import com.surftools.wimp.utils.config.IWritableConfigurationManager;

/**
 * compose maps of participation history
 */
public class ParticipantHistoryProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ParticipantHistoryProcessor.class);
  protected static LocalDate epochDate;
  private IWritableConfigurationManager cm;
  private Map<String, DogfoodEntry> dogfoodMap;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    this.cm = (IWritableConfigurationManager) cm;

    dogfoodMap = new HashMap<>();
    var dogfoodPathName = cm.getAsString(Key.PATH_DOGFOOD);
    if (dogfoodPathName != null) {
      var dogfoodPath = Path.of(dogfoodPathName);
      var fieldsList = ReadProcessor.readCsvFileIntoFieldsArray(dogfoodPath, ',', false, 1);
      for (var fields : fieldsList) {
        var entry = DogfoodEntry.fromFields(fields);
        dogfoodMap.put(entry.call, entry);
      }
      logger.info("read " + dogfoodMap.size() + " entries from " + dogfoodPathName);
    }
  }

  @Override
  public void process() {
  }

  @Override
  public void postProcess() {
    var db = new PersistenceManager(cm);
    var ret = db.getHealth();
    if (ret.status() != ReturnStatus.OK) {
      logger.error("Health Check failed: " + ret.content());
      return;
    }

    makeParticipantHistory(db);
  }

  private void makeParticipantHistory(IPersistenceManager db) {
    var epochDateString = cm.getAsString(Key.PERSISTENCE_EPOCH_DATE);
    epochDate = LocalDate.parse(epochDateString);
    logger.info("Epoch Date: " + epochDate.toString());
    var ret = db.getFilteredExercises(null, epochDate); // all types, all dates
    if (ret.status() != ReturnStatus.OK) {
      logger.error("Could not get filteredExercises from database: " + ret.content());
      return;
    }
    @SuppressWarnings("unchecked")
    var filteredExercises = (List<Exercise>) ret.data();

    ret = db.getUsersHistory(filteredExercises);
    if (ret.status() != ReturnStatus.OK) {
      logger.error("Could not get userHistory from database: " + ret.content());
      return;
    }

    @SuppressWarnings("unchecked")
    var joins = (List<JoinedUser>) ret.data();
    var histories = new ArrayList<ParticipantHistory>(joins.size());
    var extendedHistories = new ArrayList<ExtendedParticipantHistory>(joins.size());
    var summaries = new HashMap<Integer, ParticipantSummary>(filteredExercises.size());
    for (var join : joins) {
      if (join.exercises.size() > 0) {
        var count = 0;
        var firstDate = LocalDate.MAX;
        var lastDate = LocalDate.MIN;
        for (var exercise : join.exercises) {
          ++count;
          firstDate = (exercise.date().isBefore(firstDate)) ? exercise.date() : firstDate;
          lastDate = (exercise.date().isAfter(lastDate)) ? exercise.date() : lastDate;
        } // end loop over exercises;
        var ph = new ParticipantHistory(join.user.call(), count, firstDate, lastDate);
        histories.add(ph);
        summaries.put(count, summaries.getOrDefault(count, new ParticipantSummary(count, 0)).increment());
        var eph = ExtendedParticipantHistory.fromJoinedUser(join, filteredExercises);
        extendedHistories.add(eph);

        var call = join.user.call();
        var dogfood = dogfoodMap.get(call);
        if (dogfood != null) {
          dogfood = dogfood.addExtendedHistory(eph);
          dogfoodMap.put(call, dogfood);
        }
      } // end if join has exercises
    }
    logger.info("Got " + histories.size() + " particpant Histories");

    WriteProcessor.writeTable(new ArrayList<IWritableTable>(histories), dateString + "-participantHistory.csv");
    WriteProcessor.writeTable(new ArrayList<IWritableTable>(extendedHistories),
        dateString + "-extendedParticipantHistory.csv");
    WriteProcessor.writeTable(new ArrayList<IWritableTable>(summaries.values()),
        dateString + "-participantSummary.csv");
    if (dogfoodMap.size() > 0) {
      WriteProcessor.writeTable(new ArrayList<IWritableTable>(dogfoodMap.values()), dateString + "-dogfood.csv");
    }
  }

  static record ParticipantHistory(String call, int count, LocalDate firstDate, LocalDate lastDate)
      implements IWritableTable {

    @Override
    public int compareTo(IWritableTable other) {
      var o = (ParticipantHistory) other;
      return call.compareTo(o.call);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Call", "Count", "First", "Last" };
    }

    @Override
    public String[] getValues() {
      return new String[] { call, String.valueOf(count), firstDate.toString(), lastDate.toString() };
    }
  }

  static record ParticipantSummary(int nEvents, int nParticipants) implements IWritableTable {

    @Override
    public int compareTo(IWritableTable other) {
      var o = (ParticipantSummary) other;
      return o.nEvents - nEvents;
    }

    public ParticipantSummary increment() {
      return new ParticipantSummary(nEvents, nParticipants + 1);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Events", "Participants" };
    }

    @Override
    public String[] getValues() {
      return new String[] { String.valueOf(nEvents), String.valueOf(nParticipants) };
    }
  }

  static record ExtendedParticipantHistory(String call, String latitude, String longitude, //
      String firstDate, String lastDate, String isFirstTime, String isOneAndDone, String isCurrent, //
      String exerciseCount, String totalExercises, String exercisesSinceJoin, //
      String percentAllExercises, String percentExercisesSinceJoined) implements IWritableTable {

    @Override
    public int compareTo(IWritableTable other) {
      var o = (ExtendedParticipantHistory) other;
      return call.compareTo(o.call);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Call", "Latitude", "Longitude", //
          "First Date", "Last Date", "FirstTime?", "OneAndDone?", "Current?", //
          "ExerciseCount", "TotalExercises", "ExercisesSinceJoin", //
          "% All Exercises", "% Exercises Since Joined" };
    }

    @Override
    public String[] getValues() {
      return new String[] { call, latitude, longitude, //
          firstDate, lastDate, isFirstTime, isOneAndDone, isCurrent, //
          exerciseCount, totalExercises, exercisesSinceJoin, //
          percentAllExercises, percentExercisesSinceJoined };
    }

    public static ExtendedParticipantHistory fromJoinedUser(JoinedUser j, List<Exercise> filteredExercises) {
      var exerciseDate = date;
      var firstDate = j.dateJoined.isBefore(epochDate) ? epochDate : j.dateJoined;
      var lastDate = j.lastExerciseDate;
      var isFirstTime = String.valueOf(false);
      var isOneAndDone = String.valueOf(false);
      var isCurrent = String.valueOf(exerciseDate.compareTo(lastDate) == 0);
      var count = j.exercises.size();
      var nExercises = filteredExercises.size();
      var totalExercises = String.valueOf(nExercises);
      var nExercisesSinceJoined = filteredExercises.stream().filter(ex -> !ex.date().isBefore(j.dateJoined)).toList()
          .size();
      var exercisesSinceJoined = String.valueOf(nExercisesSinceJoined);
      var percentAllExercises = String.format("%.02f", 100d * count / nExercises);
      var percentExercisesSinceJointed = String.format("%.02f", 100d * count / nExercisesSinceJoined);
      if (j.exercises.size() == 1) {
        if (lastDate.compareTo(exerciseDate) == 0) {
          isFirstTime = String.valueOf(true);
        } else {
          isOneAndDone = String.valueOf(true);
        }
      }

      return new ExtendedParticipantHistory(j.user.call(), j.location.getLatitude(), j.location.getLongitude(), //
          firstDate.toString(), lastDate.toString(), isFirstTime, isOneAndDone, isCurrent, //
          String.valueOf(count), totalExercises, exercisesSinceJoined, //
          percentAllExercises, percentExercisesSinceJointed);

    }

  }

  static record DogfoodEntry(String email, String call, String firstName, String lastName, String state,
      String isWriter, String isTechTeam, ExtendedParticipantHistory history) implements IWritableTable {

    @Override
    public int compareTo(IWritableTable other) {
      var o = (DogfoodEntry) other;
      return email.compareTo(o.email);
    }

    @Override
    public String[] getHeaders() {
      var headers = new String[] { "Email", "Callsign", "FirstName", "LastName", "State", "IsWriter", "IsTech" };
      var exHeaders = new String[] { "Call", "Latitude", "Longitude", //
          "First Date", "Last Date", "FirstTime?", "OneAndDone?", "Current?", //
          "ExerciseCount", "TotalExercises", "ExercisesSinceJoin", //
          "% All Exercises", "% Exercises Since Joined" };

      String[] result = Stream.concat(Arrays.stream(headers), Arrays.stream(exHeaders)).toArray(String[]::new);
      return result;
    }

    @Override
    public String[] getValues() {
      var values = new String[] { email, call, firstName, lastName, state, isWriter, isTechTeam };
      var exValues = (history == null)//
          ? new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "" } //
          : history.getValues();
      String[] result = Stream.concat(Arrays.stream(values), Arrays.stream(exValues)).toArray(String[]::new);
      return result;
    }

    public static DogfoodEntry fromFields(String[] fields) {
      var email = fields[0];
      var call = fields[1].toUpperCase();
      var firstName = fields[2];
      var lastName = fields[3];
      var state = fields[4];
      var isWriter = fields[5];
      var isTechTeam = fields[6];
      var entry = new DogfoodEntry(email, call, firstName, lastName, state, isWriter, isTechTeam, null);
      return entry;
    }

    public DogfoodEntry addExtendedHistory(ExtendedParticipantHistory history) {
      return new DogfoodEntry(email, call, firstName, lastName, state, isWriter, isTechTeam, history);
    }
  }
}
