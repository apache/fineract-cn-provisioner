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
package org.apache.fineract.cn.provisioner.internal.repository;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import org.apache.fineract.cn.provisioner.config.ProvisionerConstants;
import org.apache.fineract.cn.provisioner.internal.util.DataSourceUtils;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.UUID;
import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.cn.api.util.ApiConstants;
import org.apache.fineract.cn.cassandra.core.CassandraSessionProvider;
import org.apache.fineract.cn.cassandra.util.CassandraConnectorConstants;
import org.apache.fineract.cn.crypto.HashGenerator;
import org.apache.fineract.cn.crypto.SaltGenerator;
import org.apache.fineract.cn.postgresql.util.PostgreSQLConstants;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.util.EncodingUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

@SuppressWarnings({"SqlNoDataSourceInspection", "SqlDialectInspection", "unused"})
@Component
public class ProvisionerInitializer {

  private final Environment environment;
  private final Logger logger;
  private final CassandraSessionProvider cassandraSessionProvider;
  private final SaltGenerator saltGenerator;
  private final HashGenerator hashGenerator;
  private final String initialClientId;
  private String metaKeySpaceName;

  @Autowired
  public ProvisionerInitializer(final Environment environment, @Qualifier(ProvisionerConstants.LOGGER_NAME) final Logger logger,
                                final CassandraSessionProvider cassandraSessionProvider,
                                final SaltGenerator saltGenerator, final HashGenerator hashGenerator,
                                @Value("${system.initialclientid}") final String initialClientId) {
    super();
    this.environment = environment;
    this.logger = logger;
    this.cassandraSessionProvider = cassandraSessionProvider;
    this.saltGenerator = saltGenerator;
    this.hashGenerator = hashGenerator;
    this.initialClientId = initialClientId;
  }

  @PostConstruct
  public void initialize() {
    try {
      metaKeySpaceName = this.environment.getProperty(
          CassandraConnectorConstants.KEYSPACE_PROP,
          CassandraConnectorConstants.KEYSPACE_PROP_DEFAULT);

      this.initializeCassandra();
      this.initializeDatabase(PostgreSQLConstants.POSTGRESQL_DATABASE_NAME_DEFAULT);
      this.createTableTenants();
    } catch (final Exception ex) {
      throw new IllegalStateException("Could not initialize service!", ex);
    }

  }

  private void initializeCassandra() throws Exception {
    final Session session = this.cassandraSessionProvider.getAdminSession();
    final KeyspaceMetadata keyspaceMetadata = session.getCluster().getMetadata().getKeyspace(metaKeySpaceName);

    if (keyspaceMetadata.getTable(ConfigEntity.TABLE_NAME) == null) {
      //create config family
      final String createConfigTableStatement = SchemaBuilder.createTable(ConfigEntity.TABLE_NAME)
          .addPartitionKey(ConfigEntity.NAME_COLUMN, DataType.text())
          .addColumn(ConfigEntity.SECRET_COLUMN, DataType.blob())
          .buildInternal();

      session.execute(createConfigTableStatement);

      while(keyspaceMetadata.getTable(ConfigEntity.TABLE_NAME) == null) {
        logger.debug("Waiting for config table to get created.");
      }

      final byte[] secret = this.saltGenerator.createRandomSalt();
      final BoundStatement configBoundStatement = session.prepare("INSERT INTO config (name, secret) VALUES (?, ?)").bind();
      configBoundStatement.setString("name", ProvisionerConstants.CONFIG_INTERNAL);
      configBoundStatement.setBytes("secret", ByteBuffer.wrap(secret));
      session.execute(configBoundStatement);

      //create users family
      final String createUsersTableStatement = SchemaBuilder.createTable(UserEntity.TABLE_NAME)
          .addPartitionKey(UserEntity.NAME_COLUMN, DataType.text())
          .addColumn(UserEntity.PASSWORD_COLUMN, DataType.blob())
          .addColumn(UserEntity.SALT_COLUMN, DataType.blob())
          .addColumn(UserEntity.ITERATION_COUNT_COLUMN, DataType.cint())
          .addColumn(UserEntity.EXPIRES_IN_DAYS_COLUMN, DataType.cint())
          .addColumn(UserEntity.PASSWORD_RESET_ON_COLUMN, DataType.timestamp())
          .buildInternal();

      session.execute(createUsersTableStatement);

      while(keyspaceMetadata.getTable(UserEntity.TABLE_NAME) == null) {
        logger.debug("Waiting for user table to get created.");
      }

      final String username = ApiConstants.SYSTEM_SU;
      final byte[] hashedPassword = Base64Utils.decodeFromString(ProvisionerConstants.INITIAL_PWD);
      final byte[] variableSalt = this.saltGenerator.createRandomSalt();
      final BoundStatement userBoundStatement =
          session.prepare("INSERT INTO users (name, passwordWord, salt, iteration_count, password_reset_on) VALUES (?, ?, ?, ?, ?)").bind();
      userBoundStatement.setString("name", username);
      userBoundStatement.setBytes("passwordWord", ByteBuffer.wrap(
          this.hashGenerator.hash(Base64Utils.encodeToString(hashedPassword), EncodingUtils.concatenate(variableSalt, secret),
              ProvisionerConstants.ITERATION_COUNT, ProvisionerConstants.HASH_LENGTH)));
      userBoundStatement.setBytes("salt", ByteBuffer.wrap(variableSalt));
      userBoundStatement.setInt("iteration_count", ProvisionerConstants.ITERATION_COUNT);
      userBoundStatement.setTimestamp("password_reset_on", new Date());
      session.execute(userBoundStatement);

      //create tenants family
      final String createTenantsTableStatement = SchemaBuilder.createTable(TenantEntity.TABLE_NAME)
          .addPartitionKey(TenantEntity.IDENTIFIER_COLUMN, DataType.text())
          .addColumn(TenantEntity.CLUSTER_NAME_COLUMN, DataType.text())
          .addColumn(TenantEntity.CONTACT_POINTS_COLUMN, DataType.text())
          .addColumn(TenantEntity.KEYSPACE_NAME_COLUMN, DataType.text())
          .addColumn(TenantEntity.REPLICATION_TYPE_COLUMN, DataType.text())
          .addColumn(TenantEntity.REPLICAS_COLUMN, DataType.text())
          .addColumn(TenantEntity.NAME_COLUMN, DataType.text())
          .addColumn(TenantEntity.DESCRIPTION_COLUMN, DataType.text())
          .addColumn(TenantEntity.IDENTITY_MANAGER_APPLICATION_NAME_COLUMN, DataType.text())
          .addColumn(TenantEntity.IDENTITY_MANAGER_APPLICATION_URI_COLUMN, DataType.text())
          .buildInternal();

      session.execute(createTenantsTableStatement);


      //create services family
      final String createApplicationsTableStatement =
          SchemaBuilder.createTable(ApplicationEntity.TABLE_NAME)
              .addPartitionKey(ApplicationEntity.NAME_COLUMN, DataType.text())
              .addColumn(ApplicationEntity.DESCRIPTION_COLUMN, DataType.text())
              .addColumn(ApplicationEntity.VENDOR_COLUMN, DataType.text())
              .addColumn(ApplicationEntity.HOMEPAGE_COLUMN, DataType.text())
              .buildInternal();

      session.execute(createApplicationsTableStatement);


      //create org.apache.fineract.cn.provisioner.tenant services family
      final String createTenantApplicationsTableStatement =
          SchemaBuilder.createTable(TenantApplicationEntity.TABLE_NAME)
              .addPartitionKey(TenantApplicationEntity.TENANT_IDENTIFIER_COLUMN, DataType.text())
              .addColumn(TenantApplicationEntity.ASSIGNED_APPLICATIONS_COLUMN, DataType.set(DataType.text()))
              .buildInternal();

      session.execute(createTenantApplicationsTableStatement);


      //create clients family
      final String createClientsTableStatement =
          SchemaBuilder.createTable(ClientEntity.TABLE_NAME)
              .addPartitionKey(ClientEntity.NAME_COLUMN, DataType.text())
              .addColumn(ClientEntity.DESCRIPTION_COLUMN, DataType.text())
              .addColumn(ClientEntity.REDIRECT_URI_COLUMN, DataType.text())
              .addColumn(ClientEntity.VENDOR_COLUMN, DataType.text())
              .addColumn(ClientEntity.HOMEPAGE_COLUMN, DataType.text())
              .buildInternal();
      session.execute(createClientsTableStatement);
      final String clientId = StringUtils.isEmpty(initialClientId) ? UUID.randomUUID().toString() : initialClientId;
      this.logger.info(clientId);

      while(keyspaceMetadata.getTable(ClientEntity.TABLE_NAME) == null) {
        logger.debug("Waiting for client table to get created.");
      }

      final BoundStatement clientBoundStatement = session.prepare("INSERT INTO clients (name, description, vendor, homepage) VALUES (?, ?, ?, ?)").bind();
      clientBoundStatement.setString("name", clientId);
      clientBoundStatement.setString("description", "REST Console");
      clientBoundStatement.setString("vendor", "The Apache Software Foundation");
      clientBoundStatement.setString("homepage", "https://fineract.apache.org");
      session.execute(clientBoundStatement);
    }
  }

  private void initializeDatabase(String metaDatabaseName) throws Exception {

    this.logger.info("Creating meta database {} ", metaDatabaseName);
    try (
            final Connection connection = DataSourceUtils.createProvisionerConnection(this.environment, "postgres");
            final Statement testStatement = connection.createStatement();
            final Statement statement = connection.createStatement()
            ) {
      final ResultSet validityQuery = testStatement.executeQuery("SELECT 1");
      if (validityQuery.next()){
        this.logger.info("Connection to database postgres established");
        final ResultSet findDB = statement.executeQuery("SELECT datname FROM pg_database WHERE datname = '" + metaDatabaseName + "'");
        if (!findDB.next()) {
          this.logger.info("Database {} does not exists, creating the database {} now.", metaDatabaseName);
          statement.execute("CREATE DATABASE " + metaDatabaseName);
        } else {
          this.logger.info("Database {} already exists.", metaDatabaseName);
        }
      } else {
        this.logger.warn("Could not connect to database postgres");
        throw new IllegalMonitorStateException("Could not connect to database postgres");
      }
    }
  }

  private void createTableTenants() throws SQLException {
    final String databaseName = PostgreSQLConstants.POSTGRESQL_DATABASE_NAME_DEFAULT;

    this.logger.info("Create tenants table in database {} if it does not exists", databaseName);
    try (
            final Connection provisionerConnection = DataSourceUtils.createProvisionerConnection(this.environment, databaseName);
            final Statement testStatement = provisionerConnection.createStatement();
            final Statement findSeshatStatement = provisionerConnection.createStatement()
    ) {
      final ResultSet validityQuery = testStatement.executeQuery("SELECT 1");
      if (validityQuery.next()) {
        this.logger.info("Connection to database {} established", databaseName);
        final ResultSet resultSet = findSeshatStatement.executeQuery("SELECT datname FROM pg_database where datname = '"+ databaseName +"'");
        if (resultSet.next()) {
          this.logger.info("Database {} exists !", databaseName);
          this.logger.info("Creating table tenants now");
          findSeshatStatement.execute("CREATE TABLE IF NOT EXISTS tenants (identifier VARCHAR(32) NOT NULL, driver_class VARCHAR(255) NOT NULL, database_name VARCHAR(32) NOT NULL, host VARCHAR(512) NOT NULL, port VARCHAR(5) NOT NULL, a_user VARCHAR(32) NOT NULL, pwd VARCHAR(32) NOT NULL, PRIMARY KEY (identifier))");
        } else {
          this.logger.warn("Database {} does not exists !", databaseName);
          }
      } else {
        this.logger.warn("Could not connect to database seshat");
        throw new IllegalMonitorStateException("Could not connect to database seshat");
      }
    } catch (SQLException sqlex) {
      this.logger.error(sqlex.getMessage(), sqlex);
      throw new IllegalStateException("Could not create table tenants");
    }
  }
}
