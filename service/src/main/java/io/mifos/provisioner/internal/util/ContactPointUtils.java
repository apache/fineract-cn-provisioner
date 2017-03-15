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
package io.mifos.provisioner.internal.util;

import com.datastax.driver.core.Cluster;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class ContactPointUtils {

  private ContactPointUtils() {
    super();
  }

  public static void process(final Cluster.Builder clusterBuilder, final String contactPoints) {
    final String[] splitContactPoints = contactPoints.split(",");
    for (final String contactPoint : splitContactPoints) {
      if (contactPoint.contains(":")) {
        final String[] address = contactPoint.split(":");
        clusterBuilder.addContactPointsWithPorts(
            new InetSocketAddress(address[0].trim(), Integer.valueOf(address[1].trim())));
      } else {
        try {
          clusterBuilder.addContactPoints(InetAddress.getByName(contactPoint.trim()));
        } catch (final UnknownHostException uhex) {
          throw new IllegalArgumentException("Host not found!", uhex);
        }
      }
    }
  }
}
