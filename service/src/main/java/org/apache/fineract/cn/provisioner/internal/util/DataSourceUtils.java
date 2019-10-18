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

import org.apache.fineract.cn.postgresql.util.PostgreSQLConstants;
import org.springframework.core.env.Environment;
import org.apache.fineract.cn.postgresql.util.JdbcUrlBuilder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.fineract.cn.provisioner.api.v1.domain.DatabaseConnectionInfo;

public class DataSourceUtils {

  private DataSourceUtils() {
    super();
  }

  public static Connection create(final DatabaseConnectionInfo databaseConnectionInfo) {
    try {
      Class.forName(databaseConnectionInfo.getDriverClass());
    } catch (ClassNotFoundException cnfex) {
      throw new IllegalArgumentException(cnfex.getMessage(), cnfex);
    }

    final String jdbcUrl = JdbcUrlBuilder
        .create(JdbcUrlBuilder.DatabaseType.POSTGRESQL)
        .host(databaseConnectionInfo.getHost())
        .port(databaseConnectionInfo.getPort())
        .instanceName(databaseConnectionInfo.getDatabaseName())
        .build();
    try {
      try {
        Class.forName("org.postgresql.Driver");
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
      final Connection connection = DriverManager.getConnection(jdbcUrl, databaseConnectionInfo.getUser(), databaseConnectionInfo.getPassword());
      connection.setAutoCommit(true);
      return connection;
    } catch (SQLException sqlex) {
      throw new IllegalStateException(sqlex.getMessage(), sqlex);
    }
  }

  public static Connection createProvisionerConnection(final Environment environment, String databaseName) {
    final DatabaseConnectionInfo databaseConnectionInfo = new DatabaseConnectionInfo();
    databaseConnectionInfo.setDriverClass(environment.getProperty("postgresql.driverClass"));
    if (databaseName != null) {
      databaseConnectionInfo.setDatabaseName(databaseName);
    }
    databaseConnectionInfo.setHost(environment.getProperty("postgresql.host"));
    databaseConnectionInfo.setPort(environment.getProperty("postgresql.port"));
    databaseConnectionInfo.setUser(environment.getProperty("postgresql.user"));
    databaseConnectionInfo.setPassword(environment.getProperty("postgresql.password"));

    try {
      final Connection connection = DataSourceUtils.create(databaseConnectionInfo);
      connection.setAutoCommit(true);
      return connection;
    } catch (SQLException error) {
      throw new IllegalStateException(error.getMessage(), error);
    }
  }
}
