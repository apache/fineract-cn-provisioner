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
package io.mifos.provisioner.internal.service;

import io.mifos.anubis.api.v1.domain.ApplicationSignatureSet;
import io.mifos.anubis.repository.TenantAuthorizationDataRepository;
import io.mifos.core.lang.AutoTenantContext;
import io.mifos.core.lang.ServiceException;
import io.mifos.provisioner.api.v1.domain.CassandraConnectionInfo;
import io.mifos.provisioner.api.v1.domain.DatabaseConnectionInfo;
import io.mifos.provisioner.api.v1.domain.Tenant;
import io.mifos.provisioner.config.ProvisionerConstants;
import io.mifos.provisioner.internal.repository.TenantCassandraRepository;
import io.mifos.provisioner.internal.repository.TenantDAO;
import io.mifos.provisioner.internal.repository.TenantEntity;
import io.mifos.provisioner.internal.service.applications.IdentityServiceInitializer;
import io.mifos.provisioner.internal.util.DataSourceUtils;
import io.mifos.provisioner.internal.util.DataStoreOption;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"SqlNoDataSourceInspection", "SqlDialectInspection"})
@Component
public class TenantService {
  private static final String META_KEYSPACE = "seshat"; //TODO: read MariaDB name from the configuration.

  private final Logger logger;
  private final Environment environment;
  private final TenantApplicationService tenantApplicationService;
  private final TenantAuthorizationDataRepository tenantAuthorizationDataRepository;
  private final TenantCassandraRepository tenantCassandraRepository;
  private final IdentityServiceInitializer identityServiceInitializer;


  @Autowired
  public TenantService(@Qualifier(ProvisionerConstants.LOGGER_NAME) final Logger logger,
                       final Environment environment,
                       final TenantApplicationService tenantApplicationService,
                       @SuppressWarnings("SpringJavaAutowiringInspection") final TenantAuthorizationDataRepository tenantAuthorizationDataRepository,
                       final TenantCassandraRepository tenantCassandraRepository,
                       final IdentityServiceInitializer identityServiceInitializer) {
    super();
    this.logger = logger;
    this.environment = environment;
    this.tenantApplicationService = tenantApplicationService;
    this.tenantAuthorizationDataRepository = tenantAuthorizationDataRepository;
    this.tenantCassandraRepository = tenantCassandraRepository;
    this.identityServiceInitializer = identityServiceInitializer;
  }

  public void create(final Tenant tenant) {
    this.initializeKeyspace(tenant);
    this.initializeDatabase(tenant);
  }

  private void initializeKeyspace(final @Nonnull Tenant tenant) {
    final DataStoreOption dataStoreOption = DataStoreOption.valueOf(
            this.environment.getProperty(DataStoreOption.PROPERTY_NAME, DataStoreOption.PROPERTY_DEFAULT_VALUE));
    if (dataStoreOption.isEnabled(DataStoreOption.CASSANDRA)) {
      final CassandraConnectionInfo cassandraConnectionInfo = tenant.getCassandraConnectionInfo();

      final TenantEntity tenantEntity = new TenantEntity();
      tenantEntity.setIdentifier(tenant.getIdentifier());
      tenantEntity.setClusterName(cassandraConnectionInfo.getClusterName());
      tenantEntity.setContactPoints(cassandraConnectionInfo.getContactPoints());
      tenantEntity.setKeyspaceName(cassandraConnectionInfo.getKeyspace());
      tenantEntity.setReplicationType(cassandraConnectionInfo.getReplicationType());
      tenantEntity.setReplicas(cassandraConnectionInfo.getReplicas());
      tenantEntity.setName(tenant.getName());
      tenantEntity.setDescription(tenant.getDescription());
      tenantEntity.setIdentityManagerApplicationName(null); //Identity manager can't be spun up till the io.mifos.provisioner.tenant is provisioned.
      tenantEntity.setIdentityManagerApplicationUri(null); //Identity manager can't be spun up till the io.mifos.provisioner.tenant is provisioned.

      tenantCassandraRepository.create(tenantEntity);
    }
  }

  public Optional<String> assignIdentityManager(
          final String tenantIdentifier,
          final String identityManagerAppName,
          final String identityManagerUri)
  {
    tenantCassandraRepository.adjust(tenantIdentifier, x -> {
      x.setIdentityManagerApplicationName(identityManagerAppName);
      x.setIdentityManagerApplicationUri(identityManagerUri);
    });

    IdentityServiceInitializer.IdentityServiceInitializationResult identityServiceInitializationResult = identityServiceInitializer.initializeIsis(tenantIdentifier, identityManagerAppName, identityManagerUri);
    final ApplicationSignatureSet identityServiceTenantSignatureSet = identityServiceInitializationResult.getSignatureSet();

    try (final AutoTenantContext ignored = new AutoTenantContext(tenantIdentifier)) {
      tenantAuthorizationDataRepository.createSignatureSet(identityServiceTenantSignatureSet.getTimestamp(), identityServiceTenantSignatureSet.getIdentityManagerSignature());
    }

    return identityServiceInitializationResult.getAdminPassword();
  }

  public List<Tenant> fetchAll() {
    final ArrayList<Tenant> result = new ArrayList<>();
    this.fetchAllCassandra(result);
    this.fetchAllDatabase(result);
    return result;
  }

  private void fetchAllCassandra(final @Nonnull List<Tenant> tenants) {
    final DataStoreOption dataStoreOption = DataStoreOption.valueOf(
            this.environment.getProperty(DataStoreOption.PROPERTY_NAME, DataStoreOption.PROPERTY_DEFAULT_VALUE));
    if (dataStoreOption.isEnabled(DataStoreOption.CASSANDRA)) {
      List<TenantEntity> tenantEntities = tenantCassandraRepository.fetchAll();

      for (final TenantEntity tenantEntity : tenantEntities) {
        final Tenant tenant = new Tenant();
        tenants.add(tenant);
        tenant.setIdentifier(tenantEntity.getIdentifier());
        tenant.setName(tenantEntity.getName());
        tenant.setDescription(tenantEntity.getDescription());

        tenant.setCassandraConnectionInfo(getCassandraConnectionInfoFromTenantEntity(tenantEntity));
      }
    }
  }

  @SuppressWarnings("UnnecessaryLocalVariable")
  public Optional<Tenant> find(final @Nonnull String identifier) {
    final Optional<Tenant> tenantInCassandra = this.findCassandra(identifier);
    final Optional<Tenant> tenantInDatabase = tenantInCassandra.map(x -> this.findInDatabase(x, identifier));

    return tenantInDatabase;
  }

  public void delete(final String identifier) {
    this.deleteFromCassandra(identifier);
    this.deleteDatabase(identifier);
  }

  private void fetchAllDatabase(final ArrayList<Tenant> tenants) {
    final DataStoreOption dataStoreOption = DataStoreOption.valueOf(
        this.environment.getProperty(DataStoreOption.PROPERTY_NAME, DataStoreOption.PROPERTY_DEFAULT_VALUE));
    if (dataStoreOption.isEnabled(DataStoreOption.RDBMS)) {
      if (tenants.size() > 0) {
        try (final Connection connection = DataSourceUtils.createProvisionerConnection(this.environment)) {
          for (final Tenant tenant : tenants) {
            final Optional<TenantDAO> optionalTenantDAO = TenantDAO.find(connection, tenant.getIdentifier());
            if (optionalTenantDAO.isPresent()) {
              tenant.setDatabaseConnectionInfo(optionalTenantDAO.get().map());
            }
          }
        } catch (final SQLException sqlex) {
          this.logger.error(sqlex.getMessage(), sqlex);
          throw new IllegalStateException("Could not load io.mifos.provisioner.tenant data!");
        }
      } else {
        try (final Connection connection = DataSourceUtils.createProvisionerConnection(this.environment)) {
          final List<TenantDAO> tenantDAOs = TenantDAO.fetchAll(connection);
          for (final TenantDAO tenantDAO : tenantDAOs) {
            final Tenant tenant = new Tenant();
            tenants.add(tenant);
            tenant.setIdentifier(tenantDAO.getIdentifier());
            tenant.setDatabaseConnectionInfo(tenantDAO.map());
          }
        } catch (final SQLException sqlex) {
          this.logger.error(sqlex.getMessage(), sqlex);
          throw new IllegalStateException("Could not load io.mifos.provisioner.tenant data!");
        }
      }
    }
  }

  private Optional<Tenant> findCassandra(final String identifier) {
    final DataStoreOption dataStoreOption = DataStoreOption.valueOf(
        this.environment.getProperty(DataStoreOption.PROPERTY_NAME, DataStoreOption.PROPERTY_DEFAULT_VALUE));
    if (dataStoreOption.isEnabled(DataStoreOption.CASSANDRA)) {
      return tenantCassandraRepository.get(identifier).map(x -> {
                final Tenant tenant = new Tenant();
                tenant.setIdentifier(x.getIdentifier());
                tenant.setName(x.getName());
                tenant.setDescription(x.getDescription());
                tenant.setCassandraConnectionInfo(getCassandraConnectionInfoFromTenantEntity(x));
                return tenant;
              }
      );
    }
    return Optional.empty();
  }

  private Tenant findInDatabase(final @Nonnull Tenant tenant, final @Nonnull String identifier) {
    final DataStoreOption dataStoreOption = DataStoreOption.valueOf(
        this.environment.getProperty(DataStoreOption.PROPERTY_NAME, DataStoreOption.PROPERTY_DEFAULT_VALUE));
    if (dataStoreOption.isEnabled(DataStoreOption.RDBMS)) {
      try (final Connection connection = DataSourceUtils.createProvisionerConnection(this.environment)) {
        final Optional<TenantDAO> optionalTenantDAO = TenantDAO.find(connection, identifier);
        if (optionalTenantDAO.isPresent()) {
          tenant.setDatabaseConnectionInfo(optionalTenantDAO.get().map());
          return tenant;
        }
      } catch (final SQLException sqlex) {
        this.logger.error(sqlex.getMessage(), sqlex);
        throw new IllegalStateException("Could not load io.mifos.provisioner.tenant data!");
      }
    }
    return tenant;
  }

  private void initializeDatabase(final Tenant tenant) {
    final DataStoreOption dataStoreOption = DataStoreOption.valueOf(
        this.environment.getProperty(DataStoreOption.PROPERTY_NAME, DataStoreOption.PROPERTY_DEFAULT_VALUE));
    if (dataStoreOption.isEnabled(DataStoreOption.RDBMS)) {

      try (
              final Connection provisionerConnection = DataSourceUtils.createProvisionerConnection(this.environment);
              final Statement statement = provisionerConnection.createStatement()
      ) {
        final java.sql.ResultSet resultSet = statement.executeQuery(
            " SELECT * FROM " + META_KEYSPACE + ".tenants WHERE identifier = '" + tenant.getIdentifier() + "' ");
        if (resultSet.next()) {
          throw ServiceException.conflict("Tenant {0} already exists!", tenant.getIdentifier());
        }
      } catch (SQLException sqlex) {
        this.logger.error(sqlex.getMessage(), sqlex);
        throw new IllegalStateException("Could not insert io.mifos.provisioner.tenant info!", sqlex);
      }
      final DatabaseConnectionInfo databaseConnectionInfo = tenant.getDatabaseConnectionInfo();
      try (
          final Connection connection = DataSourceUtils.create(databaseConnectionInfo);
          final Statement statement = connection.createStatement()
      ) {
        statement.execute("CREATE DATABASE IF NOT EXISTS " + databaseConnectionInfo.getDatabaseName());
        statement.close();
      } catch (final SQLException sqlex) {
        this.logger.error(sqlex.getMessage(), sqlex);
        throw new IllegalStateException("Could not create database!", sqlex);
      }

      try (final Connection provisionerConnection = DataSourceUtils.createProvisionerConnection(this.environment)) {
        final TenantDAO tenantDAO = new TenantDAO();
        tenantDAO.setIdentifier(tenant.getIdentifier());
        tenantDAO.setDriverClass(databaseConnectionInfo.getDriverClass());
        tenantDAO.setDatabaseName(databaseConnectionInfo.getDatabaseName());
        tenantDAO.setHost(databaseConnectionInfo.getHost());
        tenantDAO.setPort(databaseConnectionInfo.getPort());
        tenantDAO.setUser(databaseConnectionInfo.getUser());
        tenantDAO.setPassword(databaseConnectionInfo.getPassword());
        tenantDAO.insert(provisionerConnection);
        provisionerConnection.commit();
      } catch (SQLException sqlex) {
        this.logger.error(sqlex.getMessage(), sqlex);
        throw new IllegalStateException("Could not insert io.mifos.provisioner.tenant info!", sqlex);
      }
    }
  }

  private void deleteFromCassandra(final @Nonnull String identifier) {
    final DataStoreOption dataStoreOption = DataStoreOption.valueOf(
        this.environment.getProperty(DataStoreOption.PROPERTY_NAME, DataStoreOption.PROPERTY_DEFAULT_VALUE));
    if (dataStoreOption.isEnabled(DataStoreOption.CASSANDRA)) {
      final Optional<TenantEntity> tenantEntity = tenantCassandraRepository.get(identifier);
      tenantEntity.ifPresent(x ->
      {
        tenantCassandraRepository.delete(identifier);
        this.tenantApplicationService.deleteTenant(identifier);
      });
    }
  }

  private void deleteDatabase(final String identifier) {
    final DataStoreOption dataStoreOption = DataStoreOption.valueOf(
        this.environment.getProperty(DataStoreOption.PROPERTY_NAME, DataStoreOption.PROPERTY_DEFAULT_VALUE));
    if (dataStoreOption.isEnabled(DataStoreOption.RDBMS)) {

      try (final Connection provisionerConnection = DataSourceUtils.createProvisionerConnection(this.environment)) {
        final Optional<TenantDAO> optionalTenantDAO = TenantDAO.find(provisionerConnection, identifier);
        if (optionalTenantDAO.isPresent()) {
          final DatabaseConnectionInfo databaseConnectionInfo = optionalTenantDAO.get().map();
          try (
              final Connection connection = DataSourceUtils.create(databaseConnectionInfo);
              final Statement dropStatement = connection.createStatement()
          ) {
            dropStatement.execute("DROP DATABASE " + databaseConnectionInfo.getDatabaseName());
            connection.commit();
          }
          TenantDAO.delete(provisionerConnection, identifier);
          provisionerConnection.commit();
        }
      } catch (final SQLException sqlex) {
        this.logger.error(sqlex.getMessage(), sqlex);
        throw new IllegalStateException("Could not delete database!");
      }
    }
  }

  private static CassandraConnectionInfo getCassandraConnectionInfoFromTenantEntity(final TenantEntity tenantEntity) {
    final CassandraConnectionInfo cassandraConnectionInfo = new CassandraConnectionInfo();
    cassandraConnectionInfo.setClusterName(tenantEntity.getClusterName());
    cassandraConnectionInfo.setContactPoints(tenantEntity.getContactPoints());
    cassandraConnectionInfo.setKeyspace(tenantEntity.getKeyspaceName());
    cassandraConnectionInfo.setReplicationType(tenantEntity.getReplicationType());
    cassandraConnectionInfo.setReplicas(tenantEntity.getReplicas());
    return cassandraConnectionInfo;
  }
}
