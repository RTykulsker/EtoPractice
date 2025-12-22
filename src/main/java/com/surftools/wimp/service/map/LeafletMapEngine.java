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

package com.surftools.wimp.service.map;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class LeafletMapEngine implements IMapService {
  private static final Logger logger = LoggerFactory.getLogger(LeafletMapEngine.class);

  final Map<String, String> rgbMap = Map
      .ofEntries( //
          Map.entry("blue", "269bcc"), //
          Map.entry("gold", "ffcd44"), //
          Map.entry("red", "d84560"), //
          Map.entry("green", "23b844"), //
          Map.entry("orange", "d7884e"), //
          Map.entry("yellow", "d4bf55"), //
          Map.entry("violet", "a152ca"), //
          Map.entry("grey", "8a8a8a"), //
          Map.entry("black", "000000") //
      );

  final Set<String> ALL_ICON_COLORS = Set
      .of("blue", "gold", "red", "green", "orange", "yellow", "violet", "grey", "black");

  final Set<String> VALID_ICON_COLORS = Set // no grey
      .of("blue", "gold", "red", "green", "orange", "yellow", "violet", "black");

  public LeafletMapEngine(IConfigurationManager cm, IMessageManager mm) {
  }

  public static String escapeForJavaScript(String input) {
    if (input == null) {
      return null;
    }
    // First escape backslashes to avoid double escaping
    String escaped = input.replace("\\", "\\\\");
    // Then escape apostrophes (0x27)
    escaped = escaped.replace("'", "\\'");
    // Optionally escape newlines if embedding directly
    escaped = escaped.replace("\n", "\\n").replace("\r", "\\r");
    return escaped;
  }

  @Override
  public void makeMap(Path outputPath, MapHeader mapHeader, List<MapEntry> entries) {
    var sb = new StringBuilder();

    var labelIndex = 0;
    for (var entry : entries) {
      var color = entry.iconColor() == null ? "blue" : entry.iconColor();
      if (!ALL_ICON_COLORS.contains(color)) {
        throw new RuntimeException("mapEntry: " + entry + ", invalid color: " + color);
      }
      var point = new String(POINT_TEMPLATE);
      point = point.replaceAll("#LABEL_INDEX#", "label_" + labelIndex++);
      point = point.replaceAll("#LABEL#", entry.label());
      point = point.replace("#LATITUDE#", entry.location().getLatitude());
      point = point.replace("#LONGITUDE#", entry.location().getLongitude());
      point = point.replace("#COLOR#", color);
      var message = entry.message().replaceAll("\n", "<br/>");
      message = escapeForJavaScript(message);
      point = point.replace("#CONTENT#", message);
      sb.append(point + "\n");
    }

    var legendHTML = mapHeader.legendHTML();

    var fileContent = new String(FILE_TEMPLATE);
    fileContent = fileContent.replaceAll("#TITLE#", mapHeader.mapTitle());
    fileContent = fileContent.replace("#POINTS#", sb.toString());
    fileContent = fileContent.replace("#LEGEND_HTML#", legendHTML);

    var filePath = Path.of(outputPath.toString(), "leaflet-" + mapHeader.fileName() + ".html");
    try {
      Files.writeString(filePath, fileContent.toString());
      logger.info("wrote " + entries.size() + " entries to: " + filePath.toString());
    } catch (Exception e) {
      logger.error("Exception writing leaflet file: " + filePath.toString() + ", " + e.getMessage());
    }
  }

  private static final String POINT_TEMPLATE = """
      const #LABEL_INDEX# = L.marker([#LATITUDE#, #LONGITUDE#],{icon: #COLOR#Icon})
        .bindTooltip("#LABEL#",{permanent: true,direction: 'bottom', className: "my-labels"})
        .bindPopup('<b>#LABEL#</b><br/>#CONTENT#')
        .addTo(map);
      """;

  private static final String FILE_TEMPLATE = """
      <!DOCTYPE html>
      <html lang="en">
      <head>
        <base target="_top">
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <title>#TITLE#</title>

          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
           integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY="
           crossorigin="">

          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"
           integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo="
           crossorigin=""></script>

        <style>
          html, body {
            height: 100%;
            width: 100%;
            margin: 0;
          }

          .leaflet-container {
              height: 1000px;
              width: 2000px;
              max-width: 100%;
              max-height: 100%;
          }

          .leaflet-tooltip.my-labels {
              background-color: transparent;
              border: transparent;
              box-shadow: none;
              font-weight: bold;
              font-size: 14px;
          }

          .leaflet-popup-tip {
              background: rgba(0, 0, 0, 0) !important;
              box-shadow: none !important;
          }

          .leaflet-tooltip-top:before,
          .leaflet-tooltip-bottom:before,
          .leaflet-tooltip-left:before,
          .leaflet-tooltip-right:before {
              border: none !important;
          }

          /* Main legend box */
          .leaflet-custom-legend {
            background: white;
            padding: 8px;
            border-radius: 4px;
            box-shadow: 0 0 8px rgba(0,0,0,0.3);
            font-family: sans-serif;
            font-size: 13px;
            resize: both;
            overflow: auto;
            max-width: 260px;
            max-height: 300px;
            position: relative;
          }

          /* Header with close button */
          .leaflet-custom-legend-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-weight: bold;
            margin-bottom: 6px;
          }

          .leaflet-custom-legend-close {
            cursor: pointer;
            border: none;
            background: transparent;
            font-size: 16px;
            line-height: 1;
            padding: 0 4px;
          }

          .leaflet-custom-legend-close:hover {
            background: #eee;
          }

          /* Toggle button */
          .legend-toggle-btn {
            background: white;
            padding: 4px 6px;
            border-radius: 4px;
            box-shadow: 0 0 6px rgba(0,0,0,0.3);
            cursor: pointer;
            font-size: 12px;
          }

          .legend .box {
            display: inline-block;
            width: 14px;
            height: 14px;
            margin-right: 6px;
            vertical-align: middle;
          }


        </style>

      </head>
      <body>

      <div id="map"></div>
      <script>

        const map = L.map('map').setView([40, -91], 4);

        // ------------------------------
        // Legend Control
        // ------------------------------
        let legendContainer;  // <-- store reference so toggle can reopen it

        const LegendControl = L.Control.extend({
          options: { position: 'bottomleft' },

          onAdd: function (map) {
            legendContainer = L.DomUtil.create('div', 'leaflet-custom-legend');

            L.DomEvent.disableClickPropagation(legendContainer);
            L.DomEvent.disableScrollPropagation(legendContainer);

            // Header
            const header = L.DomUtil.create('div', 'leaflet-custom-legend-header', legendContainer);
            header.innerHTML = `<span>#TITLE#</span>`;

            const closeBtn = L.DomUtil.create('button', 'leaflet-custom-legend-close', header);
            closeBtn.innerHTML = '&times;';

            closeBtn.addEventListener('click', () => {
              legendContainer.style.display = 'none';
              toggleButton.style.display = 'block';
            });

            // Body
            const body = L.DomUtil.create('div', '', legendContainer);
            body.innerHTML = `#LEGEND_HTML#`;

            return legendContainer;
          }
        });

        map.addControl(new LegendControl());

        // ------------------------------
        // Toggle Button Control
        // ------------------------------
        let toggleButton;

        const ToggleLegendControl = L.Control.extend({
          options: { position: 'bottomleft' },

          onAdd: function (map) {
            toggleButton = L.DomUtil.create('div', 'legend-toggle-btn');
            toggleButton.innerHTML = '#TITLE#';

            L.DomEvent.disableClickPropagation(toggleButton);

            toggleButton.addEventListener('click', () => {
              legendContainer.style.display = 'block';
              toggleButton.style.display = 'none';
            });

            return toggleButton;
          }
        });

        map.addControl(new ToggleLegendControl());

        // Hide toggle button initially
        toggleButton.style.display = 'none';

        const iconSizeX = [13,21];

        const blueIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-blue.png', iconSize: iconSizeX});
        const goldIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-gold.png', iconSize: iconSizeX});
        const redIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png', iconSize: iconSizeX});
        const greenIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-green.png', iconSize: iconSizeX});
        const orangeIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-orange.png', iconSize: iconSizeX});
        const yellowIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-yellow.png', iconSize: iconSizeX});
        const violetIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-violet.png', iconSize: iconSizeX});
        const greyIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-grey.png', iconSize: iconSizeX});
        const blackIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-black.png', iconSize: iconSizeX});

        const tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
          maxZoom: 19,
          attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        }).addTo(map);

        #POINTS#

      </script>
      </body>
      </html>
            """;

  @Override
  public String makeLegendForFeedbackCount(int participantCount, Counter counter) {
    var legendHTML = "For " + participantCount + " participants";

    var it = counter.getAscendingKeyIterator();
    var sb = new StringBuilder();
    while (it.hasNext()) {
      var entry = it.next();
      sb.append("value" + ": " + entry.getKey() + ", " + "count" + ": " + entry.getValue() + "<br>");
    }
    legendHTML += "<br><br>Feedback Count:<br>" + sb.toString();
    return legendHTML;
  }

}
