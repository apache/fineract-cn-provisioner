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
package io.mifos.provisioner.internal.repository;

import io.mifos.provisioner.api.v1.domain.DatabaseConnectionInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
public class TenantDAO {

  private static final class Builder {

    private final ResultSet resultSet;

    private Builder(final ResultSet resultSet) {
      super();
      this.resultSet = resultSet;
    }

    Optional<TenantDAO> build() throws SQLException {
      if (this.resultSet.next()) {
        final TenantDAO tenantDAO = new TenantDAO();
        tenantDAO.setIdentifier(this.resultSet.getString("identifier"));
        tenantDAO.setDriverClass(this.resultSet.getString("driver_class"));
        tenantDAO.setDatabaseName(this.resultSet.getString("database_name"));
        tenantDAO.setHost(this.resultSet.getString("host"));
        tenantDAO.setPort(this.resultSet.getString("port"));
        tenantDAO.setUser(this.resultSet.getString("a_user"));
        tenantDAO.setPassword(this.resultSet.getString("pwd"));
        return Optional.of(tenantDAO);
      } else {
        return Optional.empty();
      }
    }

    List<TenantDAO> collect() throws SQLException {
      final ArrayList<TenantDAO> tenantDAOs = new ArrayList<>();
      while (this.resultSet.next()) {
        final TenantDAO tenantDAO = new TenantDAO();
        tenantDAOs.add(tenantDAO);
        tenantDAO.setIdentifier(this.resultSet.getString("identifier"));
        tenantDAO.setDriverClass(this.resultSet.getString("driver_class"));
        tenantDAO.setDatabaseName(this.resultSet.getString("database_name"));
        tenantDAO.setHost(this.resultSet.getString("host"));
        tenantDAO.setPort(this.resultSet.getString("port"));
        tenantDAO.setUser(this.resultSet.getString("a_user"));
        tenantDAO.setPassword(this.resultSet.getString("pwd"));
      }
      return tenantDAOs;
    }
  }

  private static final int INDEX_IDENTIFIER = 1;
  private static final int INDEX_DRIVER_CLASS = 2;
  private static final int INDEX_DATABASE_NAME = 3;
  private static final int INDEX_HOST = 4;
  private static final int INDEX_PORT = 5;
  private static final int INDEX_USER = 6;
  private static final int INDEX_PASSWORD = 7;

  private static final String TABLE_NAME = "tenants";
  private static final String META_KEYSPACE = "seshat"; //TODO: read MariaDB name from the configuration.
  private static final String FETCH_ALL_STMT = " SELECT * FROM " +
      META_KEYSPACE +
      "." +
      TenantDAO.TABLE_NAME;
  private static final String FIND_ONE_STMT = " SELECT * FROM " +
      META_KEYSPACE +
      "." +
      TenantDAO.TABLE_NAME +
      " WHERE identifier = ?";
  private static final String INSERT_STMT = " INSERT INTO " +
      META_KEYSPACE +
      "." +
      TenantDAO.TABLE_NAME +
      " (identifier, driver_class, database_name, host, port, a_user, pwd) " +
      " values " +
      " (?, ?, ?, ?, ?, ?, ?) ";
  private static final String DELETE_STMT = " DELETE FROM " +
      META_KEYSPACE +
      "." +
      TenantDAO.TABLE_NAME +
      " WHERE identifier = ? ";

  private String identifier;
  private String driverClass;
  private String databaseName;
  private String host;
  private String port;
  private String user;
  private String password;

  public TenantDAO() {
    super();
  }

  private static Builder create(final ResultSet resultSet) {
    return new Builder(resultSet);
  }

  public static Optional<TenantDAO> find(final Connection connection, final String identifier) throws SQLException {
    try (final PreparedStatement findOneTenantStatement = connection.prepareStatement(TenantDAO.FIND_ONE_STMT)) {
      findOneTenantStatement.setString(INDEX_IDENTIFIER, identifier);
      try (final ResultSet resultSet = findOneTenantStatement.executeQuery()) {
        return TenantDAO.create(resultSet).build();
      }
    }
  }

  public static List<TenantDAO> fetchAll(final Connection connection) throws SQLException {
    try (final Statement fetchAllTenantsStatement = connection.createStatement()) {
      try (final ResultSet resultSet = fetchAllTenantsStatement.executeQuery(TenantDAO.FETCH_ALL_STMT)) {
        return TenantDAO.create(resultSet).collect();
      }
    }
  }

  public static void delete(final Connection connection, final String identifier) throws SQLException {
    try (final PreparedStatement deleteTenantStatement = connection.prepareStatement(TenantDAO.DELETE_STMT)) {
      deleteTenantStatement.setString(INDEX_IDENTIFIER, identifier);
      deleteTenantStatement.execute();
    }
  }

  public void insert(final Connection connection) throws SQLException {
    try (final PreparedStatement insertTenantStatement = connection.prepareStatement(TenantDAO.INSERT_STMT)) {
      insertTenantStatement.setString(INDEX_IDENTIFIER, this.getIdentifier());
      insertTenantStatement.setString(INDEX_DRIVER_CLASS, this.getDriverClass());
      insertTenantStatement.setString(INDEX_DATABASE_NAME, this.getDatabaseName());
      insertTenantStatement.setString(INDEX_HOST, this.getHost());
      insertTenantStatement.setString(INDEX_PORT, this.getPort());
      insertTenantStatement.setString(INDEX_USER, this.getUser());
      insertTenantStatement.setString(INDEX_PASSWORD, this.getPassword());
      insertTenantStatement.execute();
    }
  }

  public DatabaseConnectionInfo map() {
    final DatabaseConnectionInfo databaseConnectionInfo = new DatabaseConnectionInfo();
    databaseConnectionInfo.setDriverClass(this.getDriverClass());
    databaseConnectionInfo.setDatabaseName(this.getDatabaseName());
    databaseConnectionInfo.setHost(this.getHost());
    databaseConnectionInfo.setPort(this.getPort());
    databaseConnectionInfo.setUser(this.getUser());
    databaseConnectionInfo.setPassword(this.getPassword());
    return databaseConnectionInfo;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  private String getDriverClass() {
    return driverClass;
  }

  public void setDriverClass(String driverClass) {
    this.driverClass = driverClass;
  }

  private String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  private String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  private String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }

  private String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  private String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
