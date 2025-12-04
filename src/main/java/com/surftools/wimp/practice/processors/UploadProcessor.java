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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * upload results, via FTP or whatever to shared drive
 */
public class UploadProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(UploadProcessor.class);
  @SuppressWarnings("unused")
  private String dateString = null;

  private FTPClient ftp;

//
//  ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));

  private boolean isInitialized = false;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    dateString = cm.getAsString(Key.EXERCISE_DATE);

//    https://dlptest.com/ftp-test/
    var server = "ftp.dlptest.com";
    var port = 21;
    var user = "dlpuser";
    var password = "rNrKYTX9g7z3RgJRmxWuGHbeu";

    try {
      ftp = new FTPClient();
      ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
      ftp.connect(server, port);

      int reply = ftp.getReplyCode();
      if (!FTPReply.isPositiveCompletion(reply)) {
        ftp.disconnect();
        throw new IOException("Exception in connecting to FTP Server");
      }

      boolean ret = ftp.login(user, password);
      if (!ret) {
        throw new IllegalArgumentException("failed to log in");
      }

    } catch (Exception e) {
      logger.error("Error connecting to server: " + server + ":" + port + ", " + e.getLocalizedMessage());
      isInitialized = false;
      return;
    }

    try {
      FTPFile[] files = ftp.listDirectories();
      var fileNames = Arrays.stream(files).map(FTPFile::getName).toList();
      logger.info("files: " + String.join(",", fileNames));
      isInitialized = true;
    } catch (Exception e) {
      logger.error("Error connecting to server: " + server + ":" + port + ", " + e.getLocalizedMessage());
      isInitialized = false;
      return;
    }

  }

  @Override
  public void process() {
  }

  @Override
  public void postProcess() {
    if (!isInitialized) {
      logger.info("NO ftp uploads");
      return;
    }

  } // end postProcess

}
