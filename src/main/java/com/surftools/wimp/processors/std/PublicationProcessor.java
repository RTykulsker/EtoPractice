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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * copy all files in local published/ folder to remote
 * .../results/yyyy/yyyy-mm-dd/ folder
 */
public class PublicationProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(PublicationProcessor.class);
  private boolean isInitialized = false;
  private List<String> publicationRootNamesList = new ArrayList<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    var publicationRootNames = cm.getAsString(Key.PATH_PUBLICATION);
    var fields = publicationRootNames.split(",");

    for (var publicationRootName : fields) {
      var remoteFilePath = Path.of(publicationRootName, "REMOTE");
      var remoteFile = remoteFilePath.toFile();
      if (!remoteFile.exists()) {
        logger.warn("### remote file: " + remoteFilePath.toString() + " not found.");
      } else {
        publicationRootNamesList.add(publicationRootName);
      }
    }

    isInitialized = publicationRootNamesList.size() > 0;
    if (isInitialized) {
      logger.info("will publish to the following remotes: " + String.join(", ", publicationRootNamesList));
    } else {
      logger.error("### NO remote file system not found. Can't publish");
    }
  }

  @Override
  public void process() {
  }

  @Override
  public void postProcess() {
    if (!isInitialized) {
      logger.error("### NO remote file system not found. Can't publish");
      return;
    }

    var yearString = dateString.substring(0, 4);
    for (var publicationRootName : publicationRootNamesList) {
      var remotePath = Path.of(publicationRootName, "results", yearString, dateString);
      FileUtils.makeDirIfNeeded(remotePath);
      FileUtils.copyDirectory(publishedPath, remotePath);
      logger.info("copied published local: " + publishedPath.toString() + " to remote: " + remotePath.toString());
    }
  } // end postProcess

}
