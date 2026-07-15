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

package com.surftools.wimp.core;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

/**
 * enumeration to define the various message types that we know how to handle
 *
 * @author bobt
 *
 */

public enum MessageType {

  EXPORTED(), // read but not classified
  PLAIN(), // attempted to classify, but can't infer message type
  REJECTS(), //

  FIELD_SITUATION("RMS_Express_Form_Field Situation Report", "FieldSituation"), //
  ICS_205("RMS_Express_Form_ICS205 Radio Plan_Viewer.xml", "Ics205"), //
  ICS_213("RMS_Express_Form_ICS213_Initial_Viewer.xml", "Ics213"), //
  ICS_213_RR("RMS_Express_Form_ICS213RR_Viewer.xml", "Ics213RR"), //
  HICS_259("RMS_Express_Form_HICS 259_viewer.xml", "Hics259"), //

  ;

  private final String rmsViewerName;
  private final String practiceProcessorName;

  private MessageType(String rmsViewerName, String practiceProcessorName) {
    this.rmsViewerName = rmsViewerName;
    this.practiceProcessorName = practiceProcessorName;
  }

  private MessageType() {
    this(null, null);
  }

  public static final String getAllNames() {
    var strings = new ArrayList<String>();
    for (MessageType type : values()) {
      strings.add(type.toString());
    }
    return String.join(", ", strings);
  }

  public static MessageType fromString(String string) {
    for (MessageType key : MessageType.values()) {
      if (key.toString().equals(string)) {
        return key;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return name().toLowerCase();
  }

  public String rmsViewerName() {
    return rmsViewerName;
  }

  public String getPracticeProcessorName() {
    return practiceProcessorName;
  }

  public String makeParserName() {
    var parserName = "";
    var fields = name().toLowerCase().split("_");
    for (var field : fields) {
      parserName += StringUtils.capitalize(field);
    }
    return parserName;
  }
}
