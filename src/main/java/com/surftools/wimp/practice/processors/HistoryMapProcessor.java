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

package com.surftools.wimp.practice.processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.persistence.HistoryType;
import com.surftools.wimp.persistence.JoinedUser;
import com.surftools.wimp.persistence.PersistenceManager;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.ReturnStatus;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.service.map.MapEntry;
import com.surftools.wimp.service.map.MapHeader;
import com.surftools.wimp.service.map.MapService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * compose maps of participation history
 */
public class HistoryMapProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(HistoryMapProcessor.class);
  private Set<HistoryType> historyTypesSet = new LinkedHashSet<HistoryType>(Arrays.asList(HistoryType.values()));
  private final Map<HistoryType, String> colorMap = Map
      .of(//
          HistoryType.FIRST_TIME, "gold", //
          HistoryType.ONE_AND_DONE, "red", //
          HistoryType.HEAVY_HITTER, "blue", //
          HistoryType.ALL_OTHER, "green", //
          HistoryType.FILTERED_OUT, "grey");

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {

    var historyTypesString = cm.getAsString(Key.PERSISTENCE_HISTORY_MAP_TYPES);
    if (historyTypesString != null) {
      historyTypesSet.clear();
      var fields = historyTypesString.split(",");
      for (var field : fields) {
        field = field.trim();
        var type = HistoryType.valueOf(field);
        if (type == null) {
          throw new RuntimeException("Undefined historyType: " + field);
        } else {
          historyTypesSet.add(type);
        }
      }
    }
    logger.info("HistoryTypes: " + String.join(", ", historyTypesSet.stream().map(s -> s.name()).toList()));
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

    ret = db.getUsersHistory(Set.of("Practice"), null, true);
    if (ret.status() != ReturnStatus.OK) {
      logger.error("Could not get history from database: " + ret.content());
      return;
    }

    @SuppressWarnings("unchecked")
    Map<HistoryType, List<JoinedUser>> historyMap = (Map<HistoryType, List<JoinedUser>>) ret.data();
    if (historyMap.size() == 0) {
      logger.warn("no Users with missed exercises");
      return;
    }

    var mapService = new MapService(cm, mm);
    for (var historyType : historyTypesSet) {
      var joinedUsers = historyMap.get(historyType);
      if (joinedUsers == null) {
        logger.warn("no joinedUsers for type: " + historyType.name());
        continue;
      }
      var mapEntries = new ArrayList<MapEntry>();
      for (var joinedUser : joinedUsers) {
        var context = joinedUser.context;
        @SuppressWarnings("unchecked")
        var filteredExercises = new ArrayList<Exercise>((Set<Exercise>) context);
        var label = joinedUser.user.call();
        var location = joinedUser.location;
        String message = null;
        if (historyType == HistoryType.ONE_AND_DONE || historyType == HistoryType.FIRST_TIME) {
          message = filteredExercises.size() + " exercise" + "\nDate: " + filteredExercises.getFirst().date();
        } else {
          message = filteredExercises.size() + " exercises" + "\nFirst date: "//
              + filteredExercises.getLast().date() //
              + "\nLast date: " + filteredExercises.getFirst().date();
        }
        var iconColor = colorMap.get(historyType);
        var mapEntry = new MapEntry(label, location, message, iconColor);
        mapEntries.add(mapEntry);
      }

      var title = historyType.name() + "-history";
      var description = historyType.name() + " " + cm.getAsString(Key.EXERCISE_DESCRIPTION);
      var mapHeader = new MapHeader(title, description);

      mapService.makeMap(outputPath, mapHeader, mapEntries);
    }

    if (true) {
      var x = 0;
    }
  }

}
