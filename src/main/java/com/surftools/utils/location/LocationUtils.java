/**

The MIT License (MIT)

Copyright (c) 2022, Robert Tykulsker

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

package com.surftools.utils.location;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * various static methods for location-based services
 *
 * @author bobt
 *
 */
public class LocationUtils {
  private static final Logger logger = LoggerFactory.getLogger(LocationUtils.class);

  // https://en.wikipedia.org/wiki/Great-circle_distance
  public static final double R_METERS = 6_371_009d;

  /**
   *
   * * Returns the (x, y) coordinates on the unit circle for the given index. Uses
   * binary angular subdivision: 0: 0° 1: 180° 2: 90° 3: 270° 4: 45° 5: 135° 6:
   * 225° 7: 315° 8+: recursively subdivide remaining arcs.
   *
   * @param index
   * @param center
   * @param distanceMeters
   * @return
   */

  public static LatLongPair binaryAngularSubdivision(int index, LatLongPair center, double distanceMeters) {
    if (index < 0) {
      throw new IllegalArgumentException("negative: " + index);
    }

    if (distanceMeters <= 0) {
      throw new IllegalArgumentException("non-positve distance:" + distanceMeters);
    }

    if (center == null) {
      center = new LatLongPair(0, 0);
    }

    var centerLat = center.getLatitudeAsDouble();
    var centerLon = center.getLongitudeAsDouble();

    var thetaDegrees = 0d;
    if (index == 1) {
      thetaDegrees = 180d;
    } else if (index == 2) {
      thetaDegrees = 90d;
    } else if (index == 3) {
      thetaDegrees = 270d;
    } else {
      // For index >= 4:
      // Determine which "layer" of binary subdivision this index belongs to.
      // Layer 0: 4 points (already handled)
      // Layer 1: 4 points (45°, 135°, 225°, 315°)
      // Layer 2: 8 points (22.5°, 67.5°, ...)
      // Layer 3: 16 points, etc.

      int layer = 1;
      int start = 4;
      int count = 4;

      while (index >= start + count) {
        start += count;
        count *= 2;
        layer++;
      }

      // Position inside this layer
      int pos = index - start;

      // Angle step for this layer
      double step = 360.0 / (4 * Math.pow(2, layer - 1));

      // First angle in this layer is step/2 offset from cardinal directions
      thetaDegrees = step / 2 + pos * step;
    }
    var thetaRadians = Math.toRadians(thetaDegrees);

    var lo = 0d;
    var hi = distanceMeters / 10_000d;
    var pair = (LatLongPair) null;
    var iterations = 0;
    var done = false;
    while (!done) {
      ++iterations;
      var mid = (lo + hi) / 2;
      var newLongitude = centerLon + (mid * Math.cos(thetaRadians));
      var newLatitude = centerLat + (mid * Math.sin(thetaRadians));
      pair = new LatLongPair(newLatitude, newLongitude);

      var computedDistance = computeDistanceMeters(center, pair);
      var delta = distanceMeters - computedDistance;
      if (Math.abs(delta) <= 10d || iterations >= 1000) {
        done = true;

        logger.debug("done, mid: " + mid + ", delta: " + delta + ", iterations: " + iterations);
        break;
      } else {
        logger.debug("iteration: " + iterations + ", delta: " + delta + ", lo: " + lo + ", hi: " + hi);
        if (computedDistance < distanceMeters) {
          lo = mid;
        } else {
          hi = mid;
        }
      }

    }

    return pair;
  }

//
  /**
   * see: https://gist.github.com/vananth22/888ed9a22105670e7a4092bdcf0d72e4
   *
   * @param latitude1
   * @param longitude1
   * @param latitude2
   * @param longitude2
   * @return
   */
  public static double computeDistanceMeters(double latitude1, double longitude1, double latitude2, double longitude2) {

    double lat1 = (Math.PI / 180d) * latitude1;
    double lon1 = (Math.PI / 180d) * longitude1;
    double lat2 = (Math.PI / 180d) * latitude2;
    double lon2 = (Math.PI / 180d) * longitude2;

    double latDistance = lat2 - lat1;
    double lonDistance = lon2 - lon1;
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(lat1) * Math.cos(lat2) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    double distanceMeters = R_METERS * c;
    return distanceMeters;
  }

  public static double computeDistanceMeters(LatLongPair p1, LatLongPair p2) {
    return computeDistanceMeters(p1.getLatitudeAsDouble(), p1.getLongitudeAsDouble(), p2.getLatitudeAsDouble(),
        p2.getLongitudeAsDouble());
  }

  public static boolean isValidMaidenhead(String grid) {
    if (grid == null) {
      return false;
    }

    final var pattern = "[a-rA-R]{2}[0-9]{2}[a-xA-X]{2}";
    return grid.matches(pattern);
  }

  public static double getLatitudeFromMaidenhead(String grid) {
    if (!isValidMaidenhead(grid)) {
      throw new IllegalArgumentException("grid: " + grid + " is not a valid Maidenhead grid string");
    }

    grid = grid.toUpperCase();
    double latitude = -90 + 10 * (grid.charAt(1) - 'A') + (grid.charAt(3) - '0') + 2.5 / 60 * (grid.charAt(5) - 'A')
        + 2.5 / 60 / 2;
    return latitude;
  }

  public static double getLongitudeFromMaidenhead(String grid) {
    grid = grid.toUpperCase();
    double longitude = -180 + 20 * (grid.charAt(0) - 'A') + 2 * (grid.charAt(2) - '0')
        + 5.0 / 60 * (grid.charAt(4) - 'A') + 5.0 / 60 / 2;
    return longitude;
  }

}
