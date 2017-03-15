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
package io.mifos.provisioner.tenant;


import io.mifos.provisioner.api.v1.domain.CassandraConnectionInfo;
import io.mifos.provisioner.api.v1.domain.DatabaseConnectionInfo;
import io.mifos.provisioner.api.v1.domain.Tenant;

class Fixture {

  private static Tenant compTestTenant = new Tenant();

  static final String TENANT_IDENTIFIER = "comp-test";

  static final String TENANT_NAME = "Comp Test";

  static {
    compTestTenant.setIdentifier(TENANT_IDENTIFIER);
    compTestTenant.setName(TENANT_NAME);
    compTestTenant.setDescription("Component Test Tenant");

    final CassandraConnectionInfo cassandraConnectionInfo = new CassandraConnectionInfo();
    compTestTenant.setCassandraConnectionInfo(cassandraConnectionInfo);
    cassandraConnectionInfo.setClusterName("Test Cluster");
    cassandraConnectionInfo.setContactPoints("127.0.0.1:9142");
    cassandraConnectionInfo.setKeyspace("comp_test");
    cassandraConnectionInfo.setReplicas("3");
    cassandraConnectionInfo.setReplicationType("Simple");

    final DatabaseConnectionInfo databaseConnectionInfo = new DatabaseConnectionInfo();
    compTestTenant.setDatabaseConnectionInfo(databaseConnectionInfo);
    databaseConnectionInfo.setDriverClass("org.mariadb.jdbc.Driver");
    databaseConnectionInfo.setDatabaseName("comp_test");
    databaseConnectionInfo.setHost("localhost");
    databaseConnectionInfo.setPort("3306");
    databaseConnectionInfo.setUser("root");
    databaseConnectionInfo.setPassword("mysql");
  }

  static Tenant getCompTestTenant() {
    return compTestTenant;
  }
}
