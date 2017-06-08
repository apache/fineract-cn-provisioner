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

import com.datastax.driver.core.AuthProvider;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.PlainTextAuthProvider;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.AlreadyExistsException;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import io.mifos.core.cassandra.core.CassandraSessionProvider;
import io.mifos.core.cassandra.core.ReplicationStrategyResolver;
import io.mifos.core.cassandra.util.CassandraConnectorConstants;
import io.mifos.core.lang.ServiceException;
import io.mifos.provisioner.internal.util.ContactPointUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author Myrle Krantz
 */
@Component
public class TenantCassandraRepository {
  private final Environment environment;
  private final CassandraSessionProvider cassandraSessionProvider;
  private MappingManager mappingManager;

  @Autowired
  public TenantCassandraRepository(
      final Environment environment,
      final @Nonnull CassandraSessionProvider cassandraSessionProvider) {
    super();
    this.environment = environment;
    this.cassandraSessionProvider = cassandraSessionProvider;
  }

  public Optional<TenantEntity> get(final @Nonnull String tenantIdentifier) {
    final Mapper<TenantEntity> tenantEntityMapper = this.getMappingManager().mapper(TenantEntity.class);
    return Optional.ofNullable(tenantEntityMapper.get(tenantIdentifier));
  }

  public void adjust(final @Nonnull String tenantIdentifier, final @Nonnull Consumer<TenantEntity> adjustment)
  {
    final Mapper<TenantEntity> tenantEntityMapper = this.getMappingManager().mapper(TenantEntity.class);
    final TenantEntity tenantEntity = tenantEntityMapper.get(tenantIdentifier);
    if (tenantEntity == null) {
      throw ServiceException.notFound("Tenant {0} not found!", tenantIdentifier);
    }

    adjustment.accept(tenantEntity);

    tenantEntityMapper.save(tenantEntity);
  }

  public List<TenantEntity> fetchAll() {
    final ResultSet resultSet = this.cassandraSessionProvider.getAdminSession().execute(" SELECT * FROM tenants");
    final Mapper<TenantEntity> tenantEntityMapper = this.getMappingManager().mapper(TenantEntity.class);
    final Result<TenantEntity> map = tenantEntityMapper.map(resultSet);
    return map.all();
  }

  public void delete(final @Nonnull String identifier) {
    final Mapper<TenantEntity> tenantEntityMapper = this.getMappingManager().mapper(TenantEntity.class);
    final TenantEntity tenantEntity = tenantEntityMapper.get(identifier);
    if (tenantEntity != null) {
      final Session session = this.getCluster(tenantEntity).connect();

      // drop io.mifos.provisioner.tenant keyspace
      session.execute("DROP KEYSPACE " + tenantEntity.getKeyspaceName());
      session.close();

      tenantEntityMapper.delete(identifier);
    }
  }

  public void create(final @Nonnull TenantEntity tenant) {
    final Mapper<TenantEntity> tenantEntityMapper = this.getMappingManager().mapper(TenantEntity.class);
    if (tenantEntityMapper.get(tenant.getIdentifier()) != null) {
      throw ServiceException.conflict("Tenant {0} already exists!", tenant.getIdentifier());
    }
    final Session session = this.getCluster(tenant).connect();
    try {
      session.execute("CREATE KEYSPACE " + tenant.getKeyspaceName() + " WITH REPLICATION = " +
              ReplicationStrategyResolver.replicationStrategy(
                      tenant.getReplicationType(),
                      tenant.getReplicas()));
    }
    catch (final AlreadyExistsException e) {
      throw ServiceException.badRequest("Tenant keyspace {0} already exists!", tenant.getKeyspaceName());
    }

    final String createCommandSourceTable =
            SchemaBuilder.createTable(tenant.getKeyspaceName(), "command_source")
                    .addPartitionKey("source", DataType.text())
                    .addPartitionKey("bucket", DataType.text())
                    .addClusteringColumn("created_on", DataType.timestamp())
                    .addColumn("command", DataType.text())
                    .addColumn("processed", DataType.cboolean())
                    .addColumn("failed", DataType.cboolean())
                    .addColumn("failure_message", DataType.text())
                    .buildInternal();
    session.execute(createCommandSourceTable);
    session.close();

    tenantEntityMapper.save(tenant);
  }

  private Cluster getCluster(final @Nonnull TenantEntity tenantEntity) {
    final Cluster.Builder clusterBuilder = Cluster
            .builder()
            .withClusterName(tenantEntity.getClusterName());

    if (this.environment.containsProperty(CassandraConnectorConstants.CLUSTER_USER_PROP)) {
      final String user = this.environment.getProperty(CassandraConnectorConstants.CLUSTER_USER_PROP);
      final String pwd = this.environment.getProperty(CassandraConnectorConstants.CLUSTER_PASSWORD_PROP);

      final AuthProvider authProvider = new PlainTextAuthProvider(user, pwd);
      clusterBuilder.withAuthProvider(authProvider);
    }
    ContactPointUtils.process(clusterBuilder, tenantEntity.getContactPoints());

    return clusterBuilder.build();
  }

  private MappingManager getMappingManager() {
    if (this.mappingManager == null) {
      this.mappingManager = new MappingManager(this.cassandraSessionProvider.getAdminSession());
    }
    return this.mappingManager;
  }
}
