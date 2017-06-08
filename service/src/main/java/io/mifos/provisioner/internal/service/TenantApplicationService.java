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
import io.mifos.anubis.api.v1.domain.ApplicationSignatureSet;
import io.mifos.anubis.config.TenantSignatureRepository;
import io.mifos.core.cassandra.core.CassandraSessionProvider;
import io.mifos.core.lang.AutoTenantContext;
import io.mifos.core.lang.ServiceException;
import io.mifos.provisioner.config.ProvisionerConstants;
import io.mifos.provisioner.internal.listener.EventExpectation;
import io.mifos.provisioner.internal.repository.ApplicationEntity;
import io.mifos.provisioner.internal.repository.TenantApplicationEntity;
import io.mifos.provisioner.internal.repository.TenantCassandraRepository;
import io.mifos.provisioner.internal.repository.TenantEntity;
import io.mifos.provisioner.internal.service.applications.AnubisInitializer;
import io.mifos.provisioner.internal.service.applications.IdentityServiceInitializer;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class TenantApplicationService {

  private final CassandraSessionProvider cassandraSessionProvider;
  private final AnubisInitializer anubisInitializer;
  private final IdentityServiceInitializer identityServiceInitializer;
  private final TenantSignatureRepository tenantSignatureRepository;
  private final TenantCassandraRepository tenantCassandraRepository;
  private final Logger logger;

  @Autowired
  public TenantApplicationService(final CassandraSessionProvider cassandraSessionProvider,
                                  final AnubisInitializer anubisInitializer,
                                  final IdentityServiceInitializer identityServiceInitializer,
                                  @SuppressWarnings("SpringJavaAutowiringInspection") final TenantSignatureRepository tenantSignatureRepository,
                                  final TenantCassandraRepository tenantCassandraRepository,
                                  @Qualifier(ProvisionerConstants.LOGGER_NAME) final Logger logger) {
    super();
    this.cassandraSessionProvider = cassandraSessionProvider;
    this.anubisInitializer = anubisInitializer;
    this.identityServiceInitializer = identityServiceInitializer;
    this.tenantSignatureRepository = tenantSignatureRepository;
    this.tenantCassandraRepository = tenantCassandraRepository;
    this.logger = logger;
  }

  @Async
  public void assign(final @Nonnull TenantApplicationEntity tenantApplicationEntity, final @Nonnull Map<String, String> appNameToUriMap) {
    Assert.notNull(tenantApplicationEntity);
    Assert.notNull(appNameToUriMap);

    final TenantEntity tenantEntity = tenantCassandraRepository.get(tenantApplicationEntity.getTenantIdentifier())
            .orElseThrow(() -> ServiceException.notFound("Tenant {0} not found.", tenantApplicationEntity.getTenantIdentifier()));

    checkApplicationsExist(tenantApplicationEntity.getApplications());

    saveTenantApplicationAssignment(tenantApplicationEntity);

    final Set<ApplicationNameToUriPair> applicationNameToUriPairs =
            getApplicationNameToUriPairs(tenantApplicationEntity, appNameToUriMap);

    final Optional<ApplicationSignatureSet> latestIdentityManagerSignatureSet = getLatestIdentityManagerSignatureSet(tenantEntity);
    latestIdentityManagerSignatureSet.ifPresent(y -> {
              try {
                initializeSecurity(tenantEntity, y, applicationNameToUriPairs);
              } catch (final InterruptedException e) {
                logger.error("Because of interruption, started but didn't finish application assignment for {} in tenant {}.",
                        appNameToUriMap.keySet(), tenantApplicationEntity.getTenantIdentifier(), e);
              }
            });
    if (!latestIdentityManagerSignatureSet.isPresent()) {
      logger.warn("No identity manager signature is available, so security is not initialized for applications {}",
              appNameToUriMap.keySet());
    }
  }

  private void initializeSecurity(final TenantEntity tenantEntity,
                                  final ApplicationSignatureSet identityManagerSignatureSet,
                                  final Set<ApplicationNameToUriPair> applicationNameToUriPairs) throws InterruptedException {
    final String tenantIdentifier = tenantEntity.getIdentifier();
    final String identityManagerApplicationName = tenantEntity.getIdentityManagerApplicationName();
    final String identityManagerApplicationUri = tenantEntity.getIdentityManagerApplicationUri();

    //Permittable groups must be posted before resource initialization occurs because resource initialization
    //may request callback from another service. For example, Services X, Y, and Identity.
    // X.initializeResources -> Y.requestCallback at X.address
    // Y.requestCallback -> Identity.requestPermission to call X.address
    // Therefore Identity must know of the permittable group for X.address before X.initializeResources is called.
    final Stream<EventExpectation> eventExpectations = applicationNameToUriPairs.stream().flatMap(x ->
            identityServiceInitializer.postApplicationPermittableGroups(
                    tenantIdentifier,
                    identityManagerApplicationName,
                    identityManagerApplicationUri,
                    x.uri).stream());
    for (final EventExpectation eventExpectation : eventExpectations.collect(Collectors.toList())) {
      if (!eventExpectation.waitForOccurrence(5, TimeUnit.SECONDS)) {
        logger.warn("Expected action in identity didn't complete {}.", eventExpectation);
      }
    }


    applicationNameToUriPairs.forEach(x -> {
      final ApplicationSignatureSet applicationSignatureSet = anubisInitializer.createSignatureSet(tenantIdentifier, x.name, x.uri, identityManagerSignatureSet.getTimestamp(), identityManagerSignatureSet.getIdentityManagerSignature());

      identityServiceInitializer.postApplicationDetails(
              tenantIdentifier,
              identityManagerApplicationName,
              identityManagerApplicationUri,
              x.name,
              x.uri,
              applicationSignatureSet);

      //InitializeResources on the service being added should occur last, for two reasons:
      // 1.) When the initialization event is put on the queue for this app/tenant combo, the app is fully ready for business.
      // 2.) If the app depeneds on the provisioning of identitypermissions in its initialization, those resources will be there.
      anubisInitializer.initializeResources(tenantIdentifier, x.name, x.uri);
    });
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

  private Optional<ApplicationSignatureSet> getLatestIdentityManagerSignatureSet(final @Nonnull TenantEntity tenantEntity) {
    try (final AutoTenantContext ignored = new AutoTenantContext(tenantEntity.getIdentifier())) {
      return tenantSignatureRepository.getLatestSignatureSet();
    }
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

  private void checkApplicationsExist(final Set<String> applications) {
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
