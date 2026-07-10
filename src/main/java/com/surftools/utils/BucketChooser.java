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
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * choose a random item, from a bucket (or bag) of choices
 *
 * This is sampling without replacement
 *
 * @author bobt
 *
 */
public class BucketChooser<T> {
  private List<T> sourceList;
  private List<T> workingList;
  private T singleton = null;
  private Random rng;

  /**
   * construct without an RNG. Items will be returned in fixed order
   *
   * @param items
   */
  public BucketChooser(List<T> items) {
    init(items);
    rng = null;
  }

  /**
   * construct with an RNG. Items will be returned in random order
   *
   * @param items
   * @param rng
   */
  public BucketChooser(List<T> items, Random rng) {
    init(items);
    this.rng = rng == null ? new Random() : rng;

    if (singleton == null) { // more than 1 item in lists
      Collections.shuffle(workingList, this.rng);
    }
  }

  private void init(List<T> items) {
    if (items == null) {
      throw new IllegalArgumentException("null objects");
    }

    if (items.size() == 0) {
      throw new IllegalArgumentException("empty objects");
    }

    if (items.size() == 1) {
      singleton = items.get(0);
    } else {
      sourceList = new ArrayList<T>(items);
      workingList = new ArrayList<T>(items);
    }
  }

  /**
   * get the "next" item. Might trigger a refresh and shuffle
   *
   * @return
   */
  public T next() {
    if (singleton != null) {
      return singleton;
    }

    var firstItem = workingList.remove(0);
    if (workingList.size() == 0) {
      workingList.addAll(sourceList);
    }

    if (rng != null && workingList.size() > 1) {
      Collections.shuffle(workingList, rng);
    }

    return firstItem;
  }

}
