/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

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

package com.surftools.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * choose a random item, based on weights
 *
 * This is sampling with replacement
 *
 * if weight associated with an item is negative, throw
 * IllegalArguementException during construction
 *
 * if weight associated with an item is zero, the item (for that weight) won't
 * EVER be chosen
 *
 * @author bobt
 *
 */
public class WeightedChooser<T> {
  private static final Logger logger = LoggerFactory.getLogger(WeightedChooser.class);

  private double sum;
  private List<WeightEntry<T>> entryList;
  private Random rng;

  /**
   * don't allow public to construct without arguments
   */
  @SuppressWarnings("unused")
  private WeightedChooser() {
  }

  /**
   * equal-weighted constructor from list of items
   *
   * @param items
   */
  public WeightedChooser(List<T> items, Random rng) {
    this.rng = rng == null ? new Random() : rng;

    Map<T, Double> map = new HashMap<>(items.size());
    for (T item : items) {
      Double count = map.getOrDefault(item, Double.valueOf(0));
      map.put(item, count + 1);
    }

    init(map);
  }

  /**
   * common initialization method, might have difference constructors in future
   *
   * @param map
   */
  private void init(Map<T, Double> map) {
    if (map == null) {
      throw new IllegalArgumentException("null map");
    }

    entryList = new ArrayList<>(map.size());
    sum = 0;
    for (Map.Entry<T, Double> mapEntry : map.entrySet()) {
      T item = mapEntry.getKey();
      Double weight = mapEntry.getValue();

      if (weight < 0) {
        throw new IllegalArgumentException("negative weight: " + weight + " not allowed for item: " + item);
      }

      if (weight == 0) {
        logger.debug("skipping item: " + item + " because weight is item");
        continue;
      }

      sum += weight;
      WeightEntry<T> weightEntry = new WeightEntry<T>(item, sum);
      entryList.add(weightEntry);
      logger.debug("added item: " + item + ", with weight: " + weight);
    }

    if (entryList.size() == 0) {
      throw new IllegalArgumentException("no items available to choose");
    }

    logger.debug("initialized with " + entryList.size() + " items");
  }

  public T next() {
    double r = sum * rng.nextDouble();
    for (WeightEntry<T> entry : entryList) {
      if (entry.sum() > r) {
        return entry.item();
      }
    }
    throw new RuntimeException("couldn't find an item");
  }

  record WeightEntry<T>(T item, double sum) {
  }
}
