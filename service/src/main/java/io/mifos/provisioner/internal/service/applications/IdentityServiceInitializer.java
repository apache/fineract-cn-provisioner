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
package io.mifos.provisioner.internal.service.applications;


import io.mifos.anubis.api.v1.client.Anubis;
import io.mifos.anubis.api.v1.domain.AllowedOperation;
import io.mifos.anubis.api.v1.domain.ApplicationSignatureSet;
import io.mifos.anubis.api.v1.domain.PermittableEndpoint;
import io.mifos.core.api.util.InvalidTokenException;
import io.mifos.core.lang.ServiceException;
import io.mifos.core.lang.TenantContextHolder;
import io.mifos.identity.api.v1.client.*;
import io.mifos.identity.api.v1.domain.CallEndpointSet;
import io.mifos.identity.api.v1.domain.Permission;
import io.mifos.identity.api.v1.domain.PermittableGroup;
import io.mifos.permittedfeignclient.api.v1.client.ApplicationPermissionRequirements;
import io.mifos.permittedfeignclient.api.v1.domain.ApplicationPermission;
import io.mifos.provisioner.config.ProvisionerConstants;
import io.mifos.provisioner.internal.listener.EventExpectation;
import io.mifos.provisioner.internal.listener.IdentityListener;
import io.mifos.tool.crypto.HashGenerator;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@Component
public class IdentityServiceInitializer {
  private final IdentityListener identityListener;
  private final ApplicationCallContextProvider applicationCallContextProvider;
  private final HashGenerator hashGenerator;
  private final Logger logger;

  @Value("${system.domain}")
  private String domain;

  public class IdentityServiceInitializationResult {
    private final ApplicationSignatureSet signatureSet;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<String> adminPassword;

    private IdentityServiceInitializationResult(final ApplicationSignatureSet signatureSet, final String adminPassword) {
      this.signatureSet = signatureSet;
      this.adminPassword = Optional.of(adminPassword);
    }

    private IdentityServiceInitializationResult(final ApplicationSignatureSet signatureSet) {
      this.signatureSet = signatureSet;
      this.adminPassword = Optional.empty();
    }

    public ApplicationSignatureSet getSignatureSet() {
      return signatureSet;
    }

    public Optional<String> getAdminPassword() {
      return adminPassword;
    }
  }

  @Autowired
  public IdentityServiceInitializer(
          final IdentityListener identityListener,
          final ApplicationCallContextProvider applicationCallContextProvider,
          final HashGenerator hashGenerator,
          @Qualifier(ProvisionerConstants.LOGGER_NAME) final Logger logger) {
    this.identityListener = identityListener;
    this.applicationCallContextProvider = applicationCallContextProvider;
    this.hashGenerator = hashGenerator;
    this.logger = logger;
  }

  public IdentityServiceInitializationResult initializeIsis(
          final @Nonnull String tenantIdentifier,
          final @Nonnull String applicationName,
          final @Nonnull String identityManagerUri) {
    try (final AutoCloseable ignored
                 = applicationCallContextProvider.getApplicationCallContext(tenantIdentifier, applicationName)) {
      final IdentityManager identityService = applicationCallContextProvider.getApplication(IdentityManager.class, identityManagerUri);
      try {
        final String randomPassword = RandomStringUtils.random(8, true, true);
        this.logger.debug("Generated password for tenant super user '{}' is '{}'.", tenantIdentifier, randomPassword);

        final byte[] salt = Base64Utils.encode(("antony" + tenantIdentifier + this.domain).getBytes());

        final String encodedPassword = Base64Utils.encodeToString(randomPassword.getBytes());

        final byte[] hash = this.hashGenerator.hash(encodedPassword, salt, ProvisionerConstants.ITERATION_COUNT, ProvisionerConstants.HASH_LENGTH);
        final String encodedPasswordHash = Base64Utils.encodeToString(hash);

        final ApplicationSignatureSet signatureSet = identityService.initialize(encodedPasswordHash);
        logger.info("Isis initialization for io.mifos.provisioner.tenant '{}' succeeded with signature set '{}'.", tenantIdentifier, signatureSet);

        return new IdentityServiceInitializationResult(signatureSet, encodedPasswordHash);
      } catch (final TenantAlreadyInitializedException aiex) {
        final ApplicationSignatureSet signatureSet = identityService.getLatestSignatureSet();
        logger.info("Isis initialization for io.mifos.provisioner.tenant '{}' failed because it was already initialized.  Pre-existing signature set '{}'.",
                tenantIdentifier, signatureSet);

        return new IdentityServiceInitializationResult(signatureSet);
      }
    } catch (final InvalidTokenException e) {
      throw ServiceException.conflict("The given identity instance didn't recognize the system token as valid.  " +
              "Perhaps the system keys for the provisioner or for the identity manager are misconfigured?");
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public List<EventExpectation> postApplicationPermittableGroups(
          final @Nonnull String tenantIdentifier,
          final @Nonnull String identityManagerApplicationName,
          final @Nonnull String identityManagerApplicationUri,
          final @Nonnull String applicationUri) {
    final List<PermittableEndpoint> permittables;
    try (final AutoCloseable ignored = applicationCallContextProvider.getApplicationCallGuestContext(tenantIdentifier)) {
      permittables = getPermittables(applicationUri);
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }

    try (final AutoCloseable ignored
                 = applicationCallContextProvider.getApplicationCallContext(tenantIdentifier, identityManagerApplicationName)) {
      final IdentityManager identityService = applicationCallContextProvider.getApplication(IdentityManager.class, identityManagerApplicationUri);

      final Stream<PermittableGroup> permittableGroups = getPermittableGroups(permittables);
      //You might look at this and wonder: "Why isn't she returning a stream here? She's just turning it back into
      //a stream on the other side..."
      //The answer is that you need the createOrFindPermittableGroup to be executed in the proper tenant context. If you
      //return the stream, the call to createOrFindPermittableGroup will be executed when the stream is itereated over.
      return permittableGroups.map(x -> createOrFindPermittableGroup(identityService, x)).collect(Collectors.toList());
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public void postApplicationDetails(
          final @Nonnull String tenantIdentifier,
          final @Nonnull String identityManagerApplicationName,
          final @Nonnull String identityManagerApplicationUri,
          final @Nonnull String applicationName,
          final @Nonnull String applicationUri,
          final @Nonnull ApplicationSignatureSet applicationSignatureSet) {
    final List<ApplicationPermission> applicationPermissionRequirements;
    try (final AutoCloseable ignored = applicationCallContextProvider.getApplicationCallGuestContext(tenantIdentifier)) {
      applicationPermissionRequirements = getApplicationPermissionRequirements(applicationName, applicationUri);
      logger.info("Application permission requirements for {} contain {}.", applicationName, applicationPermissionRequirements);

    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }

    try (final AutoCloseable ignored
                 = applicationCallContextProvider.getApplicationCallContext(tenantIdentifier, identityManagerApplicationName))
    {
      final IdentityManager identityService = applicationCallContextProvider.getApplication(IdentityManager.class, identityManagerApplicationUri);
      final EventExpectation eventExpectation = identityListener.expectApplicationSignatureSet(tenantIdentifier, applicationName, applicationSignatureSet.getTimestamp());
      identityService.setApplicationSignature(applicationName, applicationSignatureSet.getTimestamp(), applicationSignatureSet.getApplicationSignature());
      if (!eventExpectation.waitForOccurrence(5, TimeUnit.SECONDS)) {
        logger.warn("Expected action in identity didn't complete {}.", eventExpectation);
      }

      applicationPermissionRequirements.forEach(x -> createOrFindApplicationPermission(identityService, applicationName, x));

      final Stream<CallEndpointSet> callEndpoints = getCallEndpointSets(applicationPermissionRequirements);
      callEndpoints.forEach(x -> createOrFindApplicationCallEndpointSet(identityService, applicationName, x));
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  List<PermittableEndpoint> getPermittables(final @Nonnull String applicationUri)
  {
    try {
      final Anubis anubis = this.applicationCallContextProvider.getApplication(Anubis.class, applicationUri);
      return anubis.getPermittableEndpoints();
    }
    catch (final RuntimeException unexpected)
    {
      logger.error("Request for permittable endpoints to '{}' failed.", applicationUri, unexpected);
      return Collections.emptyList();
    }
  }

  private List<ApplicationPermission> getApplicationPermissionRequirements(final @Nonnull String applicationName,
                                                                           final @Nonnull String applicationUri)
  {
    try {
      final ApplicationPermissionRequirements anput
              = this.applicationCallContextProvider.getApplication(ApplicationPermissionRequirements.class, applicationUri);
      return anput.getRequiredPermissions();
    }
    catch (final RuntimeException unexpected)
    {
      logger.info("Get Required Permissions from application '{}' failed.", applicationName);
      return Collections.emptyList();
    }
  }

  static Stream<PermittableGroup> getPermittableGroups(final @Nonnull List<PermittableEndpoint> permittables)
  {
    final Map<String, Set<PermittableEndpoint>> groupedPermittables = new HashMap<>();

    permittables.forEach(x -> groupedPermittables.computeIfAbsent(x.getGroupId(), y -> new LinkedHashSet<>()).add(x));

    return groupedPermittables.entrySet().stream()
            .map(entry -> new PermittableGroup(entry.getKey(), entry.getValue().stream().collect(Collectors.toList())));
  }

  private static Stream<CallEndpointSet> getCallEndpointSets(
          final @Nonnull List<ApplicationPermission> applicationPermissionRequirements) {

    final Map<String, List<String>> permissionsGroupedByEndpointSet = applicationPermissionRequirements.stream()
            .collect(Collectors.groupingBy(ApplicationPermission::getEndpointSetIdentifier,
                    Collectors.mapping(x -> x.getPermission().getPermittableEndpointGroupIdentifier(), Collectors.toList())));

    return permissionsGroupedByEndpointSet.entrySet().stream().map(entry -> {
      final CallEndpointSet ret = new CallEndpointSet();
      ret.setIdentifier(entry.getKey());
      ret.setPermittableEndpointGroupIdentifiers(entry.getValue());
      return ret;
    });
  }

  EventExpectation createOrFindPermittableGroup(
          final @Nonnull IdentityManager identityService,
          final @Nonnull PermittableGroup permittableGroup) {
    final EventExpectation eventExpectation = identityListener.expectPermittableGroupCreation(TenantContextHolder.checkedGetIdentifier(), permittableGroup.getIdentifier());
    try {
      identityService.createPermittableGroup(permittableGroup);
      logger.info("Group '{}' creation successfully requested in identity service for tenant {}.", permittableGroup.getIdentifier(), TenantContextHolder.checkedGetIdentifier());
    }
    catch (final PermittableGroupAlreadyExistsException groupAlreadyExistsException)
    {
      //if the group already exists, read out and compare.  If the group is the same, there is nothing left to do.
      final PermittableGroup existingGroup = identityService.getPermittableGroup(permittableGroup.getIdentifier());
      if (!existingGroup.getIdentifier().equals(permittableGroup.getIdentifier())) {
        logger.error("Group '{}' already exists for tenant {}, but has a different name {} (strange).", permittableGroup.getIdentifier(), TenantContextHolder.checkedGetIdentifier(), existingGroup.getIdentifier());
        identityListener.withdrawExpectation(eventExpectation);
      }

      //Compare as sets because I'm not going to get into a hissy fit over order.
      final Set<PermittableEndpoint> existingGroupPermittables = new HashSet<>(existingGroup.getPermittables());
      final Set<PermittableEndpoint> newGroupPermittables = new HashSet<>(permittableGroup.getPermittables());
      if (!existingGroupPermittables.equals(newGroupPermittables)) {
        logger.error("Group '{}' already exists for tenant {}, but has different contents.", permittableGroup.getIdentifier(), TenantContextHolder.checkedGetIdentifier());
        identityListener.withdrawExpectation(eventExpectation);
      }
    }
    catch (final RuntimeException unexpected)
    {
      logger.error("Creating group '{}' for tenant {} failed.", permittableGroup.getIdentifier(), TenantContextHolder.checkedGetIdentifier(), unexpected);
      identityListener.withdrawExpectation(eventExpectation);
    }
    return eventExpectation;
  }

  private void createOrFindApplicationPermission(
          final @Nonnull IdentityManager identityService,
          final @Nonnull String applicationName,
          final @Nonnull ApplicationPermission applicationPermission) {
    try {
      identityService.createApplicationPermission(applicationName, applicationPermission.getPermission());
      logger.info("Application permission '{}.{}' created.",
              applicationName, applicationPermission.getPermission().getPermittableEndpointGroupIdentifier());
    }
    catch (final ApplicationPermissionAlreadyExistsException alreadyExistsException)
    {
      //if exists, read out and compare.  If is the same, there is nothing left to do.
      final Permission existing = identityService.getApplicationPermission(
              applicationName, applicationPermission.getPermission().getPermittableEndpointGroupIdentifier());
      if (!existing.getPermittableEndpointGroupIdentifier().equals(applicationPermission.getPermission().getPermittableEndpointGroupIdentifier())) {
        logger.error("Application permission '{}' already exists, but has a different name {} (strange).",
                applicationPermission.getPermission().getPermittableEndpointGroupIdentifier(), existing.getPermittableEndpointGroupIdentifier());
      }

      final Set<AllowedOperation> existingAllowedOperations = existing.getAllowedOperations();
      final Set<AllowedOperation> newAllowedOperations = applicationPermission.getPermission().getAllowedOperations();
      if (!existingAllowedOperations.equals(newAllowedOperations)) {
        logger.error("Permission '{}' already exists, but has different contents.", applicationPermission.getPermission().getPermittableEndpointGroupIdentifier());
      }
    }
    catch (final RuntimeException unexpected)
    {
      logger.error("Creating permission '{}' failed.", applicationPermission.getPermission().getPermittableEndpointGroupIdentifier(), unexpected);
    }
  }

  private void createOrFindApplicationCallEndpointSet(
          final @Nonnull IdentityManager identityService,
          final @Nonnull String applicationName,
          final @Nonnull CallEndpointSet callEndpointSet) {
    try {
      identityService.createApplicationCallEndpointSet(applicationName, callEndpointSet);
    }
    catch (final CallEndpointSetAlreadyExistsException alreadyExistsException)
    {
      //if already exists, read out and compare.  If is the same, there is nothing left to do.
      final CallEndpointSet existing = identityService.getApplicationCallEndpointSet(
              applicationName, callEndpointSet.getIdentifier());
      if (!existing.getIdentifier().equals(callEndpointSet.getIdentifier())) {
        logger.error("Application call endpoint set '{}' already exists, but has a different name {} (strange).",
                callEndpointSet.getIdentifier(), existing.getIdentifier());
      }

      //Compare as sets because I'm not going to get into a hissy fit over order.
      final Set<String> existingPermittableEndpoints = new HashSet<>(existing.getPermittableEndpointGroupIdentifiers());
      final Set<String> newPermittableEndpoints = new HashSet<>(callEndpointSet.getPermittableEndpointGroupIdentifiers());
      if (!existingPermittableEndpoints.equals(newPermittableEndpoints)) {
        logger.error("Application call endpoint set '{}' already exists, but has different contents.", callEndpointSet.getIdentifier());
      }
    }
    catch (final RuntimeException unexpected)
    {
      logger.error("Creating application call endpoint set '{}' failed.", callEndpointSet.getIdentifier(), unexpected);
    }
  }
}
