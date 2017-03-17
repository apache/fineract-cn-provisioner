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

public class JdbcUrlBuilderTest {

  private final static String MARAIDB_JDBC_URL = "jdbc:mariadb://localhost:3306/comp_test";

  public JdbcUrlBuilderTest() {
    super();
  }

  @Test
  public void shouldCreateMysqlUrl() {
    final String mariaDbJdbcUrl = JdbcUrlBuilder
        .create(JdbcUrlBuilder.DatabaseType.MARIADB)
        .host("localhost")
        .port("3306")
        .instanceName("comp_test")
        .build();

    Assert.assertEquals(MARAIDB_JDBC_URL, mariaDbJdbcUrl);
  }
}
