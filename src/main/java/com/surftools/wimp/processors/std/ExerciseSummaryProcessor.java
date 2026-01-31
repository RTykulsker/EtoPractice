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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.persistence.PersistenceManager;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.JoinedUser;
import com.surftools.wimp.persistence.dto.ReturnStatus;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * produce ExerciseSummary table and chart
 */
public class ExerciseSummaryProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ExerciseSummaryProcessor.class);

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

    var summaries = makeExerciseSummaryTable(db);
    makeExerciseSummaryChart(summaries);
  } // end postProcess

  private List<ExerciseSummary> makeExerciseSummaryTable(PersistenceManager db) {
    var summaries = new ArrayList<ExerciseSummary>();
    var exerciseCountMap = new HashMap<Long, CountEntry>();

    var ret = db.getFilteredExercises(null, null); // all types
    if (ret.status() != ReturnStatus.OK) {
      logger.error("Could not get filteredExercises from database: " + ret.content());
      return summaries;
    }
    @SuppressWarnings("unchecked")
    var filteredExercises = (List<Exercise>) ret.data();
    for (var exercise : filteredExercises) {
      exerciseCountMap.put(exercise.id(), new CountEntry(0, 0));
    }

    ret = db.getUsersHistory(filteredExercises);
    if (ret.status() != ReturnStatus.OK) {
      logger.error("Could not get userHistory from database: " + ret.content());
      return summaries;
    }

    @SuppressWarnings("unchecked")
    var joins = (List<JoinedUser>) ret.data();

    for (var join : joins) {
      for (var event : join.events) {
        var exerciseId = event.exerciseId();
        if (!exerciseCountMap.containsKey(exerciseId)) {
          continue; // not a filteredExercise
        }
        var oldCountEntry = exerciseCountMap.get(exerciseId);
        var newCountEntry = new CountEntry(oldCountEntry.userCount + 1,
            oldCountEntry.feedbackCount + event.feedbackCount());
        exerciseCountMap.put(exerciseId, newCountEntry);
      }
    }

    for (var exercise : filteredExercises) {
      var countEntry = exerciseCountMap.get(exercise.id());
      var summary = new ExerciseSummary(exercise, countEntry);
      summaries.add(summary);
    }
    WriteProcessor.writeTable(new ArrayList<IWritableTable>(summaries),
        Path.of(outputPathName, dateString + "-exerciseSummary.csv"));

    return summaries;
  }

  private void makeExerciseSummaryChart(List<ExerciseSummary> summaries) {
    Collections.sort(summaries); // latest first
    var lastDateString = summaries.get(0).exercise.date().toString();
    summaries = summaries.reversed();
    var firstDateString = summaries.get(0).exercise.date().toString();
    var dateRange = firstDateString + " - " + lastDateString;

    var dates = new ArrayList<String>();
    var userCounts = new ArrayList<String>();
    var feedbackCounts = new ArrayList<String>();
    var trainingIndices = new ArrayList<String>();
    var practiceIndices = new ArrayList<String>();
    var specialIndices = new ArrayList<String>();

    for (var i = 0; i < summaries.size(); ++i) {
      var summary = summaries.get(i);
      var exercise = summary.exercise();
      dates.add("\"" + exercise.date().toString() + "\"");
      var type = exercise.type();
      switch (type) {
      case "Practice":
        practiceIndices.add(String.valueOf(i));
        break;
      case "Training":
        trainingIndices.add(String.valueOf(i));
        break;
      case "Special":
      default:
        specialIndices.add(String.valueOf(i));
      }
      var counts = summary.counts;
      userCounts.add(String.valueOf(counts.userCount));
      var dAvg = (counts.userCount == 0) ? 0d : (double) counts.feedbackCount / (double) counts.userCount;
      var avg = String.format("%.2f", dAvg);
      feedbackCounts.add(avg);
    } // end loop over summaries

    var content = CHART_TEMPLATE;
    content = content.replace("#DATE_RANGE", dateRange);
    content = content.replace("#DATES", String.join(",", dates));
    content = content.replace("#USER_COUNTS", String.join(",", userCounts));
    content = content.replace("#FEEDBACK_COUNTS", String.join(",", feedbackCounts));
    content = content.replace("#TRAINING_INDICES", String.join(",", trainingIndices));
    content = content.replace("#PRACTICE_INDICES", String.join(",", practiceIndices));
    content = content.replace("#SPECIAL_INDICES", String.join(",", specialIndices));

    WriteProcessor.writeString(content, Path.of(outputPathName, dateString + "-exerciseSummary.html"));
  }

  static record ExerciseSummary(Exercise exercise, CountEntry counts) implements IWritableTable {
    @Override
    public int compareTo(IWritableTable other) {
      var o = (ExerciseSummary) other;
      return o.exercise.date().compareTo(date);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Date", "Type", "Name", "Description", "# Participants", "Feedback (tot)",
          "Feedback (avg)" };
    }

    @Override
    public String[] getValues() {
      var e = this.exercise;
      var c = this.counts;
      var dAvg = (c.userCount == 0) ? 0d : (double) c.feedbackCount / (double) c.userCount;
      var n = String.valueOf(c.userCount);
      var m = String.valueOf(c.feedbackCount);
      var avg = String.format("%.2f", dAvg);
      return new String[] { e.date().toString(), e.type(), e.name(), e.description(), n, m, avg };
    }
  }

  static record CountEntry(int userCount, int feedbackCount) {
  }

  private static final String CHART_TEMPLATE = """
      <!DOCTYPE html>
      <html>
      <head>
        <script src="https://cdn.plot.ly/plotly-latest.min.js"></script>
      </head>
      <body>
      <div id="chart" style="width:900px;height:550px;"></div>

      <script>
      const dates = [
      #DATES
      ];

      // --- Dataset 1 (left axis) ---
      const y1 = [#USER_COUNTS];

      // Three color groups
      const redIdx1   = [#TRAINING_INDICES];
      const blueIdx1  = [#PRACTICE_INDICES];
      const greenIdx1 = [#SPECIAL_INDICES];

      // --- Dataset 2 (right axis) ---
      const y2 = [#FEEDBACK_COUNTS];

      const redIdx2   = [#TRAINING_INDICES];
      const blueIdx2  = [#PRACTICE_INDICES];
      const greenIdx2 = [#SPECIAL_INDICES];

      // Helper
      const pick = (idx, dates, values) =>
        idx.map(i => ({ x: dates[i], y: values[i] }));

      // --- Line traces (no hover) ---
      const line1 = {
        x: dates,
        y: y1,
        mode: "lines",
        line: { color: "black", width: 2 },
        name: "User Counts",
        yaxis: "y1",
        hoverinfo: "skip"
      };

      const line2 = {
        x: dates,
        y: y2,
        mode: "lines",
        line: { color: "black", width: 2, dash: "dot" },
        name: "Avg Feedback",
        yaxis: "y2",
        hoverinfo: "skip"
      };

      // --- Marker traces for Y1 ---
      const red1 = {
        x: pick(redIdx1, dates, y1).map(p => p.x),
        y: pick(redIdx1, dates, y1).map(p => p.y),
        mode: "markers",
        marker: { color: "red", size: 10 },
        name: "Training Counts",
        yaxis: "y1"
      };

      const blue1 = {
        x: pick(blueIdx1, dates, y1).map(p => p.x),
        y: pick(blueIdx1, dates, y1).map(p => p.y),
        mode: "markers",
        marker: { color: "blue", size: 10 },
        name: "Practice Counts",
        yaxis: "y1"
      };

      const green1 = {
        x: pick(greenIdx1, dates, y1).map(p => p.x),
        y: pick(greenIdx1, dates, y1).map(p => p.y),
        mode: "markers",
        marker: { color: "green", size: 10 },
        name: "Special Counts",
        yaxis: "y1"
      };

      // --- Marker traces for Y2 ---
      const red2 = {
        x: pick(redIdx2, dates, y2).map(p => p.x),
        y: pick(redIdx2, dates, y2).map(p => p.y),
        mode: "markers",
        marker: { color: "red", size: 10, symbol: "square" },
        name: "Training  Feedback",
        yaxis: "y2"
      };

      const blue2 = {
        x: pick(blueIdx2, dates, y2).map(p => p.x),
        y: pick(blueIdx2, dates, y2).map(p => p.y),
        mode: "markers",
        marker: { color: "blue", size: 10, symbol: "square" },
        name: "Practice Feedback",
        yaxis: "y2"
      };

      const green2 = {
        x: pick(greenIdx2, dates, y2).map(p => p.x),
        y: pick(greenIdx2, dates, y2).map(p => p.y),
        mode: "markers",
        marker: { color: "green", size: 10, symbol: "square" },
        name: "Special Feedback",
        yaxis: "y2"
      };

      // --- Layout ---
      const layout = {
        title: "ETO Participation<br>#DATE_RANGE",
        xaxis: { type: "date" },

        yaxis: {
          title: "User Counts",
          side: "left"
        },

        yaxis2: {
          title: "Average Feedback",
          overlaying: "y",
          side: "right"
        },

        legend: { orientation: "h" }
      };

      Plotly.newPlot(
        "chart",
        [line1, line2, red1, blue1, green1, red2, blue2, green2],
        layout
      );
      </script>
      </body>
      </html>
          """;
}
