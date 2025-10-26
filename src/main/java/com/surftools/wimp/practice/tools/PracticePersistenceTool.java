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

package com.surftools.wimp.practice.tools;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.persistence.PersistenceManager;
import com.surftools.wimp.persistence.dto.BulkInsertEntry;
import com.surftools.wimp.persistence.dto.Event;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.ReturnStatus;
import com.surftools.wimp.persistence.dto.User;
import com.surftools.wimp.utils.config.impl.PropertyFileConfigurationManager;

public class PracticePersistenceTool {
  public static final String REFERENCE_MESSAGE_KEY = "referenceMessage";
  public static final String INSTRUCTIONS_KEY = "instructions";

  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private static final Logger logger = LoggerFactory.getLogger(PracticePersistenceTool.class);

  @Option(name = "--config", usage = "practice onfiguration file name", required = true)
  private String configurationFileName;

  public static void main(String[] args) {
    var tool = new PracticePersistenceTool();
    CmdLineParser parser = new CmdLineParser(tool);
    try {
      parser.parseArgument(args);
      tool.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  public void run() {
    logger.info("begin run");
    try {

      var cm = new PropertyFileConfigurationManager(configurationFileName, Key.values());
      var db = new PersistenceManager(cm);
      var ret = db.getHealth();
      if (ret.status() != ReturnStatus.OK) {
        logger.error("Health Check failed: " + ret.content());
        System.exit(1);
      }

      ret = db.getAllUsers();
      if (ret.status() == ReturnStatus.OK) {
        @SuppressWarnings("unchecked")
        var users = ((List<User>) (ret.data()));
        for (var user : users) {
          logger.info(user.toString());
        }
      }

      var exercise = new Exercise(-1, LocalDate.now(), "Debug", "testing", "1,2,3");
      var events = new ArrayList<Event>();
      events
          .add(new Event(-1, -1, -1, "KM6SO", new LatLongPair("47.537366", "-122.238909"), 0, "Perfect Message",
              "Testing"));
      events
          .add(new Event(-1, -1, -1, "W7OWO", new LatLongPair("45.296333 ", "-123.010632"), 2, "Some Silly Mistake",
              "Testing"));
      events
          .add(new Event(-1, -1, -1, "KW6REX", new LatLongPair("33.53 ", "-117.75"), 0, "Perfect Message", "Testing"));

      var input = new BulkInsertEntry(exercise, events);
      ret = db.bulkInsert(input);

    } catch (Exception e) {
      logger.error("Exception: " + e.getLocalizedMessage());
      e.printStackTrace();
    }
    logger.info("end run");
  }
}
