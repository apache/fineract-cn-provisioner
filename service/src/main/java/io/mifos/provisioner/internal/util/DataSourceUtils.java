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

import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import io.mifos.provisioner.api.v1.domain.DatabaseConnectionInfo;

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
        .create(JdbcUrlBuilder.DatabaseType.MARIADB)
        .host(databaseConnectionInfo.getHost())
        .port(databaseConnectionInfo.getPort())
        .build();
    try {
      final Connection connection = DriverManager.getConnection(jdbcUrl, databaseConnectionInfo.getUser(), databaseConnectionInfo.getPassword());
      connection.setAutoCommit(false);
      return connection;
    } catch (SQLException sqlex) {
      throw new IllegalStateException(sqlex.getMessage(), sqlex);
    }
  }

  public static Connection createProvisionerConnection(final Environment environment) {
    final DatabaseConnectionInfo databaseConnectionInfo = new DatabaseConnectionInfo();
    databaseConnectionInfo.setDriverClass(environment.getProperty("mariadb.driverClass"));
    databaseConnectionInfo.setHost(environment.getProperty("mariadb.host"));
    databaseConnectionInfo.setPort(environment.getProperty("mariadb.port"));
    databaseConnectionInfo.setUser(environment.getProperty("mariadb.user"));
    databaseConnectionInfo.setPassword(environment.getProperty("mariadb.password"));
    final Connection connection = DataSourceUtils.create(databaseConnectionInfo);
    try {
      connection.setAutoCommit(false);
    } catch (SQLException e) {
      // do nothing
    }
    return connection;
  }
}
