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
import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.List;

public class ContactPointUtilsTest {

  public ContactPointUtilsTest() {
    super();
  }

  @Test
  public void shouldAddSimpleContactPoints() {
    final String contactPoints = "127.0.0.1,127.0.0.2,127.0.0.3";

    final Cluster.Builder clusterBuilder = Cluster.builder();
    ContactPointUtils.process(clusterBuilder, contactPoints);
    final List<InetSocketAddress> addedClusterPoints = clusterBuilder.getContactPoints();
    Assert.assertTrue(addedClusterPoints.size() == 3);
    for (final InetSocketAddress address : addedClusterPoints) {
      Assert.assertTrue(contactPoints.contains(address.getAddress().getHostAddress()));
    }
  }

  @Test
  public void shouldAddComplexContactPoints() {
    final String contactPoints = "127.0.0.1:1234,127.0.0.2:2345,127.0.0.3:3456";

    final Cluster.Builder clusterBuilder = Cluster.builder();
    ContactPointUtils.process(clusterBuilder, contactPoints);
    final List<InetSocketAddress> addedClusterPoints = clusterBuilder.getContactPoints();

    Assert.assertTrue(addedClusterPoints.size() == 3);

    final InetSocketAddress firstAddress = addedClusterPoints.get(0);
    Assert.assertEquals("127.0.0.1", firstAddress.getAddress().getHostAddress());
    Assert.assertEquals(1234, firstAddress.getPort());

    final InetSocketAddress secondAddress = addedClusterPoints.get(1);
    Assert.assertEquals("127.0.0.2", secondAddress.getAddress().getHostAddress());
    Assert.assertEquals(2345, secondAddress.getPort());

    final InetSocketAddress thirdAddress = addedClusterPoints.get(2);
    Assert.assertEquals("127.0.0.3", thirdAddress.getAddress().getHostAddress());
    Assert.assertEquals(3456, thirdAddress.getPort());
  }
}
