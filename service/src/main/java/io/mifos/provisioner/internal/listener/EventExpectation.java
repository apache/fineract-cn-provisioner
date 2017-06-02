/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.provisioner.internal.listener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Myrle Krantz
 */
public class EventExpectation {
  private final EventKey key;
  private boolean eventFound = false;
  private boolean eventWithdrawn = false;

  private final ReentrantLock lock = new ReentrantLock();

  private final Condition found = lock.newCondition();

  EventExpectation(final EventKey key) {
    this.key = key;
  }

  EventKey getKey() {
    return key;
  }

  void setEventFound(boolean eventFound) {
    lock.lock();
    try {
      this.eventFound = eventFound;
      found.signal();
    }
    finally {
      lock.unlock();
    }
  }

  void setEventWithdrawn(boolean eventWithdrawn) {
    lock.lock();
    try {
      this.eventWithdrawn = eventWithdrawn;
      found.signal();
    }
    finally {
      lock.unlock();
    }
  }

  public boolean waitForOccurrence(long timeout, TimeUnit timeUnit) throws InterruptedException {

    lock.lock();
    try {
      if (eventFound)
      return true;

      if (eventWithdrawn)
        return false;

      found.await(timeout, timeUnit);

      return (eventFound);
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public String toString() {
    return key.toString();
  }
}
