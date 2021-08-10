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
package org.apache.fineract.cn.provisioner.internal.util;

import org.junit.Assert;
import org.junit.Test;

public class JdbcUrlBuilderTest {

  private final static String POSTGRES_DB_JDBC_URL = "jdbc:postgresql:postgres:5432/comp_test";

  public JdbcUrlBuilderTest() {
    super();
  }

  @Test
  public void shouldCreatePostgresUrl() {
    final String postgresDbUrl = JdbcUrlBuilder
        .create(JdbcUrlBuilder.DatabaseType.POSTGRESQL)
        .host("postgres")
        .port("5432")
        .instanceName("comp_test")
        .build();

    Assert.assertEquals(POSTGRES_DB_JDBC_URL, postgresDbUrl);
  }
}
