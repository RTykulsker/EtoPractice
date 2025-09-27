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
import java.util.function.Predicate;

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

  FIELD_SITUATION("RMS_Express_Form_Field Situation Report"), //
  ICS_205("RMS_Express_Form_ICS205 Radio Plan_Viewer.xml"), //
  ICS_213("RMS_Express_Form_ICS213_Initial_Viewer.xml", "ICS-213"), //
  ICS_213_RR("RMS_Express_Form_ICS213RR_Viewer.xml"), //
  HICS_259("RMS_Express_Form_HICS 259_viewer.xml"), //

  ;

  private final String rmsViewerName;
  private final String formDataName;
  private final Predicate<String> subjectPredicate;

  private MessageType(String rmsViewerName, String formDataName, Predicate<String> subjectPredicate) {
    this.rmsViewerName = rmsViewerName;
    this.formDataName = formDataName;
    this.subjectPredicate = subjectPredicate;
  }

  private MessageType() {
    this(null, null, null);
  }

  private MessageType(String attachmentName) {
    this(attachmentName, null, null);
  }

  private MessageType(String attachmentName, String formDataName) {
    this(attachmentName, formDataName, null);
  }

  private MessageType(Predicate<String> subjectPredicate) {
    this(null, null, subjectPredicate);
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

  public String formDataName() {
    return formDataName;
  }

  public boolean testSubject(String subject) {
    return subjectPredicate != null && subjectPredicate.test(subject);
  }

  public Predicate<String> getSubjectPredicate() {
    return subjectPredicate;
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
