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

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.persistence.IPersistenceManager;
import com.surftools.wimp.persistence.JoinedUser;
import com.surftools.wimp.persistence.PersistenceManager;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.ReturnStatus;
import com.surftools.wimp.service.map.MapContext;
import com.surftools.wimp.service.map.MapEntry;
import com.surftools.wimp.service.map.MapLayer;
import com.surftools.wimp.service.map.MapService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * compose maps of participation history
 */
public class HistoryMapProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(HistoryMapProcessor.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {

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
  }

  private void makeFirstTimeMap(IPersistenceManager db) {
    var ret = db.getFilteredExercises(null, date); // all types
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
    for (var join : joins) {
      if (join.exercises.size() == 1 && join.exercises.get(0).date().equals(exerciseDate)) {
        firstTimers.add(join);
      }
    }
    logger.debug("Got " + firstTimers.size() + " firstTimers ");

    var mapService = new MapService(cm, mm);
    var layers = new ArrayList<MapLayer>();
    var layer = new MapLayer("First Time Participants (" + firstTimers.size() + ")", "#ffd326");
    layers.add(layer);

    var mapEntries = new ArrayList<MapEntry>();
    for (var join : firstTimers) {
      var content = "<b>" + join.user.call() + "</b><hr>" + "First Exercise!";
      var mapEntry = new MapEntry(join.user.call(), "", join.location, content, "#ffd326");
      mapEntries.add(mapEntry);
    }

    var legendTitle = dateString + " First Time Participants (" + joins.size() + " total)";
    var context = new MapContext(publishedPath, //
        dateString + "-map-First-Timers", // file name
        dateString + " First Timer Participants", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);
  }

}
