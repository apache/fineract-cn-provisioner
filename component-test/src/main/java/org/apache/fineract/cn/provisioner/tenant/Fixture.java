/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.provisioner.tenant;


import org.apache.commons.lang.RandomStringUtils;
import org.apache.fineract.cn.postgresql.util.PostgreSQLConstants;
import org.apache.fineract.cn.provisioner.api.v1.domain.CassandraConnectionInfo;
import org.apache.fineract.cn.provisioner.api.v1.domain.DatabaseConnectionInfo;
import org.apache.fineract.cn.provisioner.api.v1.domain.Tenant;
import org.apache.fineract.cn.test.env.TestEnvironment;

class Fixture {
  static Tenant getCompTestTenant() {
    final Tenant compTestTenant = new Tenant();
    compTestTenant.setIdentifier(TestEnvironment.getRandomTenantName());
    compTestTenant.setName("Comp Test " + RandomStringUtils.randomAlphanumeric(4));
    compTestTenant.setDescription("Component Test Tenant " + RandomStringUtils.randomAlphabetic(4));

    final CassandraConnectionInfo cassandraConnectionInfo = new CassandraConnectionInfo();
    compTestTenant.setCassandraConnectionInfo(cassandraConnectionInfo);
    cassandraConnectionInfo.setClusterName("Test Cluster" + RandomStringUtils.randomAlphabetic(3));
    cassandraConnectionInfo.setContactPoints("cassandra:9042");
    cassandraConnectionInfo.setKeyspace(compTestTenant.getIdentifier());
    cassandraConnectionInfo.setReplicas("3");
    cassandraConnectionInfo.setReplicationType("Simple");

    final DatabaseConnectionInfo databaseConnectionInfo = new DatabaseConnectionInfo();
    compTestTenant.setDatabaseConnectionInfo(databaseConnectionInfo);
    databaseConnectionInfo.setDriverClass("org.postgresql.Driver");
    databaseConnectionInfo.setDatabaseName(compTestTenant.getIdentifier());
    databaseConnectionInfo.setHost("postgres");
    databaseConnectionInfo.setPort("5432");
    databaseConnectionInfo.setUser("postgres");
    databaseConnectionInfo.setPassword("postgres");

    return compTestTenant;
  }
}
