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
  WINLINK_CALLSIGN("winlink.callsign"), // mbo address

  PATH_REFERENCE("path.reference"), // dir where reference dirs/files are
  PATH_EXERCISES("path.exercises"), // dir where we find input/, output/ and published/
  PATH_RESOUCE_CONTENT("path.resourceContent"), // FILE for ICS-213 RR generation
  PATH_NAG_CONTENT("path.nagContent"), // path to FILE where end-of-year nag is
  PATH_UPLOAD_FTP_LOCAL("path.upload.ftp.local"), // path where local FTP dir is

  MAP_TEMPLATE_METHOD("map.template.method"), // "fast" or "slow", default "fast"

  GENERATOR_RNG_SEED("generator.rngSeed"), // to get consistent results
  GENERATOR_N_YEARS("generator.nYears"), // number of years to generate
  GENERATOR_INSTRUCTION_URL("generator.instruction.url"), // url at the bottom of each instruction

  ENABLE_LEGACY("enable.legacy"), // allows for 3rd week practice, instead of monthly training
  ENABLE_FINALIZE("enable.finalize"), // set to true to enable, renames output, sends email

  PERSISTENCE_SQLITE_URL("persistence.sqlite.url"), //
  PERSISTENCE_ALLOW_FUTURE("persistence.allow.future"), // allow/disallow future exercises into db
  PERSISTENCE_ONLY_USE_ACTIVE("persistence.only.use.active"), // true -> only active; false -> active & inactive
  PERSISTENCE_MISS_LIMIT("persistence.miss.limit"), // max # of missed exercises before we don't bother any more
  PERSISTENCE_MISS_SUBJECT("persistence.miss.subject"), // subject for outbound message
  PERSISTENCE_MISS_BODY_PATH("persistence.miss.body.path"), // path to file containing body for outbound message
  PERSISTENCE_EPOCH_DATE("persistence.epochDate"), // when does the database go from beta to production

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

  ALL_FEEDBACK_TEXT_EDITOR("all_feedback.textEditor"), // class name of text editor for AllFeedback
  BODY_TEXT_EDITOR("body.textEditor"), // class name of text editor for outbound message body

  EXERCISE_DATE("exerciseDate"), // for Summarizer
  EXERCISE_NAME("exerciseName"), // for Summarizer
  EXERCISE_DESCRIPTION("exerciseDescription"), // for Summarizer
  EXERCISE_WINDOW_OPEN("exerciseWindowOpen"), //
  EXERCISE_WINDOW_CLOSE("exerciseWindowClose"), //

  EMAIL_NOTIFICATION_FROM("email.notification.from"), //
  EMAIL_NOTIFICATION_TO("email.notification.to"), // comma-delimited list
  EMAIL_NOTIFICATION_PASSWORD_FILEPATH("email.notification.password.filePath"), // no password in config
  EMAIL_NOTIFICATION_SUBJECT("email.notification.subject"), // with #DATE# substitution
  EMAIL_NOTIFICATION_BODY("email.notification.body"), // with #DATE# substitution

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