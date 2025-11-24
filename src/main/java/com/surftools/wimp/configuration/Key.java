/**

The MIT License (MIT)

Copyright (c) 2019, Robert Tykulsker

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

package com.surftools.wimp.configuration;

import com.surftools.wimp.utils.config.IConfigurationKey;

/**
 * the @{IConfigurationKey} for the WinlinkMessageMapper
 *
 * @author bobt
 *
 */
public enum Key implements IConfigurationKey {
  PRACTICE_WINLINK_CALLSIGN("winlink.callsign"), // mbo address

  PRACTICE_PATH_HOME("practice.path.home"), // root; C:\ETO_Practice
  PRACTICE_PATH_REFERENCE("practice.path.reference"), // dir where reference dirs/files are
  PRACTICE_PATH_EXPORTED_MESSAGES_HOME("practice.path.exportedMessages"), // dir where we find exported messages
  PRACTICE_PATH_RESOUCE_CONTENT("practice.path.resourceContent"), // FILE for ICS-213 RR generation
  PRACTICE_PATH_NAG_CONTENT("practice.path.nagContent"), // path to FILE where end-of-year nag is

  PRACTICE_GENERATOR_RNG_SEED("practice.generator.rngSeed"), // to get consistent results
  PRACTICE_GENERATOR_N_YEARS("practice.generator.nYears"), // number of years to generate

  PRACTICE_ENABLE_LEGACY("enable.legacy"), //

  PERSISTENCE_SQLITE_URL("persistence.sqlite.url"), //
  PERSISTENCE_ALLOW_FUTURE("persistence.allow.future"), // allow/disallow future exercises into db
  PERSISTENCE_ONLY_USE_ACTIVE("persistence.only.use.active"), // true -> only active; false -> active & inactive
  PERSISTENCE_MISS_LIMIT("persistence.miss.limit"), // max # of missed exercises before we don't bother any more
  PERSISTENCE_MISS_SUBJECT("persistence.miss.subject"), // subject for outbound message
  PERSISTENCE_MISS_BODY_PATH("persistence.miss.body.path"), // path to file containing body for outbound message
  PERSISTENCE_HISTORY_MAP_TYPES("persistence.history.map.types"), // HistoryTypes to produce maps for

  PATH("path"), // path to message files
  OUTPUT_PATH("output.path"), // to override as subdir of path
  OUTPUT_PATH_CLEAR_ON_START("output.path.clearOnStart"), // if true contents of outputDir cleared

  EXPECTED_MESSAGE_TYPES("expectedMessageTypes"), // MessageTypes that we will handle

  ACKNOWLEDGEMENT_SPECIFICATION("acknowledgement.specification"), // what to acknowledge, expected vs unexpectd
  ACKNOWLEDGEMENT_EXPECTED("acknowledgement.expected"), // content for expected messages
  ACKNOWLEDGEMENT_UNEXPECTED("acknowledgement.unexpected"), // content for unexpected messages
  ACKNOWLEDGEMENT_EXTRA_CONTENT("acknowledgement.extraContent"), // extra stuff for each outbound ack message

  FILTER_INCLUDE_SENDERS("filterIncludeSenders"), // comma-delimited list of call signs to filter include
  FILTER_EXCLUDE_SENDERS("filterExcludeSenders"), // comma-delimited list of call signs to filter exclude

  DEDUPLICATION_RULES("deduplication.rules"), // json string: {messageTypeName:rule,...}

  PIPELINE_STDIN("pipeline.stdin"), // list of input processors
  PIPELINE_STDOUT("pipeline.stdout"), // list of output processors
  PIPELINE_MAIN("pipeline.main"), // list of main processors

  // PRACTICE_PATH("practice.path"), // path where practice files are written

  PRACTICE_ALL_FEEDBACK_TEXT_EDITOR("practice.all_feedback.textEditor"), // class name of text editor for AllFeedback
  PRACTICE_BODY_TEXT_EDITOR("practice.body.textEditor"), // class name of text editor for outbound message body

  EXERCISE_DATE("exerciseDate"), // for Summarizer
  EXERCISE_NAME("exerciseName"), // for Summarizer
  EXERCISE_DESCRIPTION("exerciseDescription"), // for Summarizer
  EXERCISE_WINDOW_OPEN("exerciseWindowOpen"), //
  EXERCISE_WINDOW_CLOSE("exerciseWindowClose"), //

  OUTBOUND_MESSAGE_ENGINE_TYPE("outboundMessage.engineType"), // PAT, WINLINK_CMS, etc
  OUTBOUND_MESSAGE_SOURCE("outboundMessage.source"), // mbo address
  OUTBOUND_MESSAGE_SENDER("outboundMessage.sender"), // from address
  OUTBOUND_MESSAGE_SUBJECT("outboundMessage.subject"), // message subject
  OUTBOUND_MESSAGE_EXTRA_CONTEXT("outboundMessage.extraContext"), // where to find extra context for specific engine

  CHART_CONFIG("chartConfig"), // as a JSON blob

  READ_FILTER_ENABLED("read.filterEnabled"), // to filter in/out messages by sender/from in BaseReadProcessor
  ;

  private final String key;

  private Key(String key) {
    this.key = key;
  }

  public static Key fromString(String string) {
    for (Key key : Key.values()) {
      if (key.toString().equals(string)) {
        return key;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return key;
  }
}