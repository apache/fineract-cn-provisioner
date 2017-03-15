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

import org.junit.Assert;
import org.junit.Test;

public class DataStoreOptionTest {

  public DataStoreOptionTest() {
    super();
  }

  @Test
  public void givenAllShouldBeEnabled() {
    final DataStoreOption all = DataStoreOption.ALL;
    Assert.assertTrue(all.isEnabled(DataStoreOption.CASSANDRA));
    Assert.assertTrue(all.isEnabled(DataStoreOption.RDBMS));
    Assert.assertTrue(all.isEnabled(DataStoreOption.CASSANDRA));
  }

  @Test
  public void shouldOnlyCassandraEnabled() {
    final DataStoreOption cassandra = DataStoreOption.CASSANDRA;
    Assert.assertTrue(cassandra.isEnabled(DataStoreOption.CASSANDRA));
    Assert.assertFalse(cassandra.isEnabled(DataStoreOption.RDBMS));
    Assert.assertFalse(cassandra.isEnabled(DataStoreOption.ALL));
  }

  @Test
  public void shouldOnlyRdbmsEnabled() {
    final DataStoreOption rdbms = DataStoreOption.RDBMS;
    Assert.assertFalse(rdbms.isEnabled(DataStoreOption.CASSANDRA));
    Assert.assertTrue(rdbms.isEnabled(DataStoreOption.RDBMS));
    Assert.assertFalse(rdbms.isEnabled(DataStoreOption.ALL));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailUnknownType() {
    DataStoreOption.valueOf("unknown");
    Assert.fail();
  }
}
