package com.surftools.wimp.message;

public class PracticeMessage {
  private String formVersion = "";
  private String expressVersion = "";

  @SuppressWarnings("unused")
  private PracticeMessage() {
  }

  public PracticeMessage(ExportedMessage message) {
    var type = message.getMessageType();
    switch (type) {
    case ICS_213:
      var m = (Ics213Message) message;
      formVersion = m.version;
      expressVersion = m.expressVersion;
      break;
    default:
      break;
    }
  }

  public String getFormVersion() {
    return formVersion;
  }

  public String getExpressVersion() {
    return expressVersion;
  }
}
