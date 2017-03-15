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

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.Result;

import io.mifos.anubis.api.v1.TokenConstants;
import io.mifos.anubis.api.v1.domain.Signature;
import io.mifos.anubis.repository.TenantAuthorizationDataRepository;
import io.mifos.core.cassandra.core.CassandraSessionProvider;
import io.mifos.core.lang.AutoTenantContext;
import io.mifos.core.lang.ServiceException;
import io.mifos.provisioner.internal.repository.ApplicationEntity;
import io.mifos.provisioner.internal.repository.TenantCassandraRepository;
import io.mifos.provisioner.internal.repository.TenantApplicationEntity;
import io.mifos.provisioner.internal.repository.TenantEntity;

import io.mifos.provisioner.internal.service.applications.AnubisInitializer;
import io.mifos.provisioner.internal.service.applications.IdentityServiceInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TenantApplicationService {

  private final CassandraSessionProvider cassandraSessionProvider;
  private final AnubisInitializer anubisInitializer;
  private final IdentityServiceInitializer identityServiceInitializer;
  private final TenantAuthorizationDataRepository tenantAuthorizationDataRepository;
  private final TenantCassandraRepository tenantCassandraRepository;

  @Autowired
  public TenantApplicationService(final CassandraSessionProvider cassandraSessionProvider,
                                  final AnubisInitializer anubisInitializer,
                                  final IdentityServiceInitializer identityServiceInitializer,
                                  final TenantAuthorizationDataRepository tenantAuthorizationDataRepository,
                                  final TenantCassandraRepository tenantCassandraRepository) {
    super();
    this.cassandraSessionProvider = cassandraSessionProvider;
    this.anubisInitializer = anubisInitializer;
    this.identityServiceInitializer = identityServiceInitializer;
    this.tenantAuthorizationDataRepository = tenantAuthorizationDataRepository;
    this.tenantCassandraRepository = tenantCassandraRepository;
  }

  @Async
  public void assign(final @Nonnull TenantApplicationEntity tenantApplicationEntity, final @Nonnull Map<String, String> appNameToUriMap) {
    Assert.notNull(tenantApplicationEntity);
    Assert.notNull(appNameToUriMap);

    final Optional<TenantEntity> tenantEntity = tenantCassandraRepository.get(tenantApplicationEntity.getTenantIdentifier());
    tenantEntity.ifPresent(x -> {
      checkApplications(tenantApplicationEntity.getApplications());

      saveTenantApplicationAssignment(tenantApplicationEntity);

      final Set<ApplicationNameToUriPair> applicationNameToUriPairs =
              getApplicationNameToUriPairs(tenantApplicationEntity, appNameToUriMap);

      initializeIsis(x, applicationNameToUriPairs);

      getIsisSignature(x).ifPresent(y -> initializeAnubis(x, y, applicationNameToUriPairs));
    });

    tenantEntity.orElseThrow(
            () -> ServiceException.notFound("Tenant {0} not found.", tenantApplicationEntity.getTenantIdentifier()));
  }

  private void saveTenantApplicationAssignment(final @Nonnull TenantApplicationEntity tenantApplicationEntity) {
    final Mapper<TenantApplicationEntity> tenantApplicationEntityMapper =
            this.cassandraSessionProvider.getAdminSessionMappingManager().mapper(TenantApplicationEntity.class);

    tenantApplicationEntityMapper.save(tenantApplicationEntity);
  }

  private Set<ApplicationNameToUriPair> getApplicationNameToUriPairs(
          final @Nonnull TenantApplicationEntity tenantApplicationEntity,
          final @Nonnull Map<String, String> appNameToUriMap) {
    return tenantApplicationEntity.getApplications().stream()
            .map(x -> new TenantApplicationService.ApplicationNameToUriPair(x, appNameToUriMap.get(x)))
            .collect(Collectors.toSet());
  }

  private static class ApplicationNameToUriPair
  {
    String name;
    String uri;

    ApplicationNameToUriPair(String name, String uri) {
      this.name = name;
      this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ApplicationNameToUriPair that = (ApplicationNameToUriPair) o;
      return Objects.equals(name, that.name) &&
              Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, uri);
    }
  }

  private Optional<Signature> getIsisSignature(final @Nonnull TenantEntity tenantEntity) {
    try (final AutoTenantContext ignored = new AutoTenantContext(tenantEntity.getIdentifier())) {
      return tenantAuthorizationDataRepository.getSignature(TokenConstants.VERSION);
    }
  }

  private void initializeIsis(
          final @Nonnull TenantEntity tenantEntity,
          final @Nonnull Set<ApplicationNameToUriPair> applicationNameToUriPairs) {
    applicationNameToUriPairs.forEach(applicationNameUriPair ->
            identityServiceInitializer.postPermittableGroups(
                    tenantEntity.getIdentifier(),
                    tenantEntity.getIdentityManagerApplicationName(),
                    tenantEntity.getIdentityManagerApplicationUri(),
                    applicationNameUriPair.uri));
  }

  private void initializeAnubis(
          final @Nonnull TenantEntity tenantEntity,
          final @Nonnull Signature identityServiceTenantSignature,
          final @Nonnull Set<ApplicationNameToUriPair> applicationNameToUriPairs) {
    applicationNameToUriPairs.forEach(applicationNameUriPair ->
            anubisInitializer.initializeAnubis(
                    tenantEntity.getIdentifier(),
                    applicationNameUriPair.name,
                    applicationNameUriPair.uri,
                    identityServiceTenantSignature)
    );
  }

  public TenantApplicationEntity find(final String tenantIdentifier) {
    checkTenant(tenantIdentifier);

    final Mapper<TenantApplicationEntity> tenantApplicationEntityMapper =
        this.cassandraSessionProvider.getAdminSessionMappingManager().mapper(TenantApplicationEntity.class);

    return tenantApplicationEntityMapper.get(tenantIdentifier);
  }

  void deleteTenant(final String tenantIdentifier) {
    final Mapper<TenantApplicationEntity> tenantApplicationEntityMapper =
        this.cassandraSessionProvider.getAdminSessionMappingManager().mapper(TenantApplicationEntity.class);

    tenantApplicationEntityMapper.delete(tenantIdentifier);
  }

  void removeApplication(final String name) {
    final ResultSet tenantApplicationResultSet =
        this.cassandraSessionProvider.getAdminSession().execute("SELECT * FROM tenant_applications");

    if (tenantApplicationResultSet != null) {
      final Mapper<TenantApplicationEntity> tenantApplicationEntityMapper =
          this.cassandraSessionProvider.getAdminSessionMappingManager().mapper(TenantApplicationEntity.class);

      final Result<TenantApplicationEntity> mappedTenantApplications = tenantApplicationEntityMapper.map(tenantApplicationResultSet);

      for (TenantApplicationEntity tenantApplicationEntity : mappedTenantApplications) {
        if (tenantApplicationEntity.getApplications().contains(name)) {
          tenantApplicationEntity.getApplications().remove(name);
          tenantApplicationEntityMapper.save(tenantApplicationEntity);
        }
      }
    }
  }

  private void checkApplications(final Set<String> applications) {
    final Mapper<ApplicationEntity> applicationEntityMapper =
            this.cassandraSessionProvider.getAdminSessionMappingManager().mapper(ApplicationEntity.class);

    for (final String name : applications) {
      if (applicationEntityMapper.get(name) == null) {
        throw ServiceException.badRequest("Application {0} not found!", name);
      }
    }
  }

  private void checkTenant(final @Nonnull String tenantIdentifier) {
    final Optional<TenantEntity> tenantEntity = tenantCassandraRepository.get(tenantIdentifier);
    tenantEntity.orElseThrow(() -> ServiceException.notFound("Tenant {0} not found.", tenantIdentifier));
  }
}
