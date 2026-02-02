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

package com.surftools.wimp.processors.std;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.persistence.IPersistenceManager;
import com.surftools.wimp.persistence.PersistenceManager;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.JoinedUser;
import com.surftools.wimp.persistence.dto.OneAndDone;
import com.surftools.wimp.persistence.dto.ReturnStatus;
import com.surftools.wimp.service.map.MapContext;
import com.surftools.wimp.service.map.MapEntry;
import com.surftools.wimp.service.map.MapLayer;
import com.surftools.wimp.service.map.MapService;
import com.surftools.wimp.utils.config.IConfigurationManager;
import com.surftools.wimp.utils.config.IWritableConfigurationManager;

/**
 * compose maps of participation history
 */
public class HistoryMapProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(HistoryMapProcessor.class);

  private IWritableConfigurationManager cm;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    this.cm = (IWritableConfigurationManager) cm;
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

    makeFirstTimeMap(db);
    makeCurrentMap(db);
    makeHistoricMap(db);
  }

  private void makeFirstTimeMap(IPersistenceManager db) {
    var epochDateString = cm.getAsString(Key.PERSISTENCE_EPOCH_DATE);
    var epochDate = LocalDate.parse(epochDateString);
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

    var firstTimers = new ArrayList<JoinedUser>();
    var exerciseDate = LocalDate.parse(dateString);
    var exerciseDateCount = 0;
    for (var join : joins) {
      if (join.exercises.size() > 0 && join.exercises.get(0).date().equals(exerciseDate)) {
        ++exerciseDateCount;
        if (join.exercises.size() == 1) {
          firstTimers.add(join);
        }
      }
    }
    logger.info("Got " + firstTimers.size() + " exerciseDateCount First Timers");

    var color = MapService.rgbMap.get("gold");

    var layers = new ArrayList<MapLayer>();
    var layer = new MapLayer("First Time Participants (" + firstTimers.size() + ")", color);
    layers.add(layer);

    var mapEntries = new ArrayList<MapEntry>();
    for (var join : firstTimers) {
      var content = "<b>" + join.user.call() + "</b><hr>" + "First Exercise!";
      var mapEntry = new MapEntry(join.user.call(), "", join.location, content, color);
      mapEntries.add(mapEntry);
    }

    var legendTitle = dateString + " First Time Participants (" + exerciseDateCount + " total)";

    var mapService = new MapService(cm, mm);
    var context = new MapContext(outputPath, //
        dateString + "-map-First-Timers", // file name
        dateString + " First Timer Participants", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);
  }

  String intToSuffix(int i) {
    switch (i) {
    case 1:
      return "st";
    case 2:
      return "nd";
    case 3:
      return "rd";
    default:
      return "th";
    }
  }

  private void makeCurrentMap(PersistenceManager db) {
    var ret = db.getFilteredExercises(null, null); // all types, all dates
    if (ret.status() != ReturnStatus.OK) {
      logger.error("Could not get filteredExercises from database: " + ret.content());
      return;
    }
    @SuppressWarnings("unchecked")
    var filteredExercises = (List<Exercise>) ret.data();
    var nExercises = filteredExercises.size();

    final var nBuckets = 4;
    var bucketLimits = new int[nBuckets];
    for (var i = 0; i < nBuckets; ++i) {
      var limit = ((i + 1) * nExercises) / nBuckets;
      bucketLimits[i] = limit;
    }
    var mapService = new MapService(cm, mm);
    var gradientMap = mapService.makeGradientMap(240, 120, nBuckets);
    var firstTimeColor = MapService.rgbMap.get("gold");

    ret = db.getUsersHistory(filteredExercises);
    if (ret.status() != ReturnStatus.OK) {
      logger.error("Could not get userHistory from database: " + ret.content());
      return;
    }

    @SuppressWarnings("unchecked")
    var joins = (List<JoinedUser>) ret.data();

    var firstTimeCount = 0;
    var bucketCounts = new int[nBuckets];
    var exerciseDate = LocalDate.parse(dateString);
    var exerciseDateCount = 0;
    var mapEntries = new ArrayList<MapEntry>();
    for (var join : joins) {

      @SuppressWarnings("unused")
      var debug = false;
      if (join.user.call().equals("KM6SO")) {
        debug = true;
      }

      // we only want current exercise folks
      if (join.exercises.size() > 0 && join.exercises.get(0).date().equals(exerciseDate)) {
        ++exerciseDateCount;
      } else {
        continue;
      }

      if (join.exercises.size() == 1) {
        ++firstTimeCount;
        var content = "<b>" + join.user.call() + "</b><hr>" + "First Exercise!";
        var mapEntry = new MapEntry(join.user.call(), "", join.location, content, firstTimeColor);
        mapEntries.add(mapEntry);
      } else {
        var exerciseCount = join.exercises.size();
        var found = false;
        for (var i = 0; i <= nBuckets; ++i) {
          if (exerciseCount > bucketLimits[i]) {
            continue;
          }
          ++bucketCounts[i];
          var sb = new StringBuilder("<b>" + join.user.call() + "</b><hr>");

          sb.append("Exercise Count: " + exerciseCount + " / " + nExercises + " exercises<br>");

          var rate = (100d * exerciseCount) / (nExercises);
          sb.append("Exercise rate: " + String.format("%.2f", rate) + "%<br>");
          sb.append("Last Date: " + join.exercises.get(0).date() + "<br>");
          sb.append("First Date: " + join.exercises.get(exerciseCount - 1).date() + "<br>");
          var content = sb.toString();
          var mapEntry = new MapEntry(join.user.call(), "", join.location, content, gradientMap.get(i));
          mapEntries.add(mapEntry);
          found = true;
          break;
        } // end loop over buckets
        if (!found) {
          logger.error("####### not found for call: " + join.user.call());
        }
      } // end not first-time
    } // end loop over joins

    var layers = new ArrayList<MapLayer>();
    var layer = new MapLayer("First Time Participants (" + firstTimeCount + ")", firstTimeColor);
    layers.add(layer);
    for (var i = 0; i < nBuckets; ++i) {
      var qi = i + 1;
      layer = new MapLayer(qi + intToSuffix(qi) + " Quartile (" + bucketCounts[i] + ")", gradientMap.get(i));
      layers.add(layer);
    }

    var legendTitle = dateString + " Current Exercise Participants (" + exerciseDateCount + " total)";

    var context = new MapContext(outputPath, //
        dateString + "-map-current-participants", // file name
        dateString + " Current Exercise Participants", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);
  }

  private void makeHistoricMap(PersistenceManager db) {
    var epochDateString = cm.getAsString(Key.PERSISTENCE_EPOCH_DATE);
    var epochDate = LocalDate.parse(epochDateString);
    logger.info("Epoch Date: " + epochDate.toString());
    var ret = db.getFilteredExercises(null, epochDate); // all types, all dates
    if (ret.status() != ReturnStatus.OK) {
      logger.error("Could not get filteredExercises from database: " + ret.content());
      return;
    }
    @SuppressWarnings("unchecked")
    var filteredExercises = (List<Exercise>) ret.data();
    var nExercises = filteredExercises.size();

    final var nBuckets = 4;
    var bucketLimits = new int[nBuckets];
    for (var i = 0; i < nBuckets; ++i) {
      var limit = ((i + 1) * nExercises) / nBuckets;
      bucketLimits[i] = limit;
    }
    var mapService = new MapService(cm, mm);
    var gradientMap = mapService.makeGradientMap(240, 120, nBuckets);
    var firstTimeColor = MapService.rgbMap.get("gold");
    var oneAndDoneColor = MapService.rgbMap.get("red");

    ret = db.getUsersHistory(filteredExercises);
    if (ret.status() != ReturnStatus.OK) {
      logger.error("Could not get userHistory from database: " + ret.content());
      return;
    }

    @SuppressWarnings("unchecked")
    var joins = (List<JoinedUser>) ret.data();

    var firstTimeCount = 0;
    var oneAndDoneCount = 0;
    var bucketCounts = new int[nBuckets];
    var exerciseDate = LocalDate.parse(dateString);
    var mapEntries = new ArrayList<MapEntry>();
    var oneAndDones = new ArrayList<OneAndDone>();
    var oneAndDoneMapEntries = new ArrayList<MapEntry>();
    for (var join : joins) {

      @SuppressWarnings("unused")
      var debug = false;
      var debugList = List.of("KM6SO");
      if (debugList.contains(join.user.call())) {
        debug = true;
      }

      if (join.exercises.size() == 1) {
        if (join.exercises.get(0).date().equals(exerciseDate)) {
          ++firstTimeCount;
          var content = "<b>" + join.user.call() + "</b><hr>" + "First Exercise!";
          var mapEntry = new MapEntry(join.user.call(), "", join.location, content, firstTimeColor);
          mapEntries.add(mapEntry);
        } else {
          ++oneAndDoneCount;
          var content = "<b>" + join.user.call() + "</b><hr>" + "Only Exercise: "
              + join.exercises.get(0).date().toString();
          var mapEntry = new MapEntry(join.user.call(), "", join.location, content, oneAndDoneColor);
          mapEntries.add(mapEntry);
          oneAndDoneMapEntries.add(mapEntry);

          var oneAndDoneEntry = OneAndDone.FromJoinedUser(join);
          oneAndDones.add(oneAndDoneEntry);
        }
      } else {
        var exerciseCount = join.exercises.size();
        var found = false;
        for (var i = 0; i <= nBuckets; ++i) {
          if (exerciseCount > bucketLimits[i]) {
            continue;
          }
          ++bucketCounts[i];
          var sb = new StringBuilder("<b>" + join.user.call() + "</b><hr>");

          sb.append("Exercise Count: " + exerciseCount + " / " + nExercises + " exercises<br>");

          var rate = (100d * exerciseCount) / (nExercises);
          sb.append("Exercise rate: " + String.format("%.2f", rate) + "%<br>");
          if (join.exercises.size() > 0) {
            sb.append("Last Date: " + join.exercises.get(0).date() + "<br>");
            sb.append("First Date: " + join.exercises.get(exerciseCount - 1).date() + "<br>");
          }
          var content = sb.toString();
          var mapEntry = new MapEntry(join.user.call(), "", join.location, content, gradientMap.get(i));
          mapEntries.add(mapEntry);
          found = true;
          break;
        } // end loop over buckets
        if (!found) {
          logger.error("####### not found for call: " + join.user.call());
        }
      } // end if more than one exercise
    } // end loop over joins

    var layers = new ArrayList<MapLayer>();
    var oneAndDoneLayers = new ArrayList<MapLayer>();
    var layer = new MapLayer("First Time Participants (" + firstTimeCount + ")", firstTimeColor);
    layers.add(layer);
    for (var i = 0; i < nBuckets; ++i) {
      var qi = i + 1;
      layer = new MapLayer(qi + intToSuffix(qi) + " Quartile (" + bucketCounts[i] + ")", gradientMap.get(i));
      layers.add(layer);
    }
    layer = new MapLayer("One Time Only Participants (" + oneAndDoneCount + ")", oneAndDoneColor);
    layers.add(layer);
    oneAndDoneLayers.add(layer);

    var legendTitle = dateString + " Historic Exercise Participants (" + joins.size() + " total)";

    var context = new MapContext(outputPath, //
        dateString + "-map-historic-participants", // file name
        dateString + " Historic Exercise Participants", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);

    context = new MapContext(outputPath, //
        dateString + "-map-oneAndDone", // file name
        dateString + " One Time Only Participants", // map title
        null, "One Time Only Participants", oneAndDoneLayers, oneAndDoneMapEntries);
    mapService.makeMap(context);

    WriteProcessor.writeTable(new ArrayList<IWritableTable>(oneAndDones), dateString + "-table-oneAndDone.csv");
  }

}
