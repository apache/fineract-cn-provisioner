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
package io.mifos.provisioner.internal;

import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

import io.mifos.core.cassandra.core.CassandraSessionProvider;
import io.mifos.core.cassandra.util.CassandraConnectorConstants;
import io.mifos.provisioner.AbstractServiceTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TestProvision extends AbstractServiceTest {

  public TestProvision() {
    super();
  }

  @Autowired
  protected CassandraSessionProvider cassandraSessionProvider;

  @Test
  public void dataModelExists() throws Exception {
    try (final Session session = cassandraSessionProvider.getAdminSession()) {
      final KeyspaceMetadata keyspace = session.getCluster().getMetadata().getKeyspace(CassandraConnectorConstants.KEYSPACE_PROP_DEFAULT);

      Assert.assertTrue(keyspace != null);
      Assert.assertTrue(keyspace.getTable("config") != null);
      Assert.assertTrue(keyspace.getTable("users") != null);
      Assert.assertTrue(keyspace.getTable("tenants") != null);
      Assert.assertTrue(keyspace.getTable("applications") != null);
      Assert.assertTrue(keyspace.getTable("tenant_applications") != null);
      Assert.assertTrue(keyspace.getTable("clients") != null);

      session.execute("USE " + CassandraConnectorConstants.KEYSPACE_PROP_DEFAULT);

      final ResultSet configResultSet = session.execute("SELECT * FROM config WHERE name = 'io.mifos.provisioner.internal'");
      Assert.assertNotNull(configResultSet.one());

      final ResultSet userResultSet = session.execute("SELECT * FROM users WHERE name = 'wepemnefret'");
      Assert.assertNotNull(userResultSet.one());

      final ResultSet clientResultSet = session.execute("SELECT * FROM clients");
      Assert.assertNotNull(clientResultSet.one());
    }
  }
}
