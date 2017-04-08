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
import io.mifos.anubis.api.v1.domain.PermittableEndpoint;
import io.mifos.anubis.api.v1.domain.Signature;
import io.mifos.identity.api.v1.client.IdentityManager;
import io.mifos.identity.api.v1.client.PermittableGroupAlreadyExistsException;
import io.mifos.identity.api.v1.client.TenantAlreadyInitializedException;
import io.mifos.identity.api.v1.domain.PermittableGroup;
import io.mifos.provisioner.config.ProvisionerConstants;
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
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Component
public class IdentityServiceInitializer {

  private final ApplicationCallContextProvider applicationCallContextProvider;
  private final HashGenerator hashGenerator;
  private final Logger logger;

  @Value("${system.domain}")
  private String domain;

  public class IdentityServiceInitializationResult {
    private final Signature signature;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<String> adminPassword;

    private IdentityServiceInitializationResult(final Signature signature, final String adminPassword) {
      this.signature = signature;
      this.adminPassword = Optional.of(adminPassword);
    }

    private IdentityServiceInitializationResult(final Signature signature) {
      this.signature = signature;
      this.adminPassword = Optional.empty();
    }

    public Signature getSignature() {
      return signature;
    }

    public Optional<String> getAdminPassword() {
      return adminPassword;
    }
  }

  @Autowired
  public IdentityServiceInitializer(
          final ApplicationCallContextProvider applicationCallContextProvider,
          final HashGenerator hashGenerator,
          @Qualifier(ProvisionerConstants.LOGGER_NAME) final Logger logger) {
    this.applicationCallContextProvider = applicationCallContextProvider;
    this.hashGenerator = hashGenerator;
    this.logger = logger;
  }

  public IdentityServiceInitializationResult initializeIsis(
          final @Nonnull String tenantIdentifier,
          final @Nonnull String applicationName,
          final @Nonnull String identityManagerUri) {
    try (final AutoCloseable ignored
                 = applicationCallContextProvider.getApplicationCallContext(tenantIdentifier, applicationName))
    {
      final IdentityManager identityService = applicationCallContextProvider.getApplication(IdentityManager.class, identityManagerUri);
      try {
        final String randomPassword = RandomStringUtils.random(8, true, true);
        this.logger.debug("Generated password for tenant super user '{}' is '{}'.", tenantIdentifier, randomPassword);

        final byte[] salt = Base64Utils.encode(("antony" + tenantIdentifier + this.domain).getBytes());

        final String encodedPassword = Base64Utils.encodeToString(randomPassword.getBytes());

        final byte[] hash = this.hashGenerator.hash(encodedPassword, salt, ProvisionerConstants.ITERATION_COUNT, ProvisionerConstants.HASH_LENGTH);
        final String encodedPasswordHash = Base64Utils.encodeToString(hash);

        final Signature signature = identityService.initialize(encodedPasswordHash);
        logger.info("Isis initialization for io.mifos.provisioner.tenant '{}' succeeded with signature '{}'.", tenantIdentifier, signature);

        return new IdentityServiceInitializationResult(signature, encodedPasswordHash);
      } catch (final TenantAlreadyInitializedException aiex) {
        final Signature signature = identityService.getSignature();
        logger.info("Isis initialization for io.mifos.provisioner.tenant '{}' failed because it was already initialized.  Pre-existing signature '{}'.",
                tenantIdentifier, signature);

        return new IdentityServiceInitializationResult(signature);
      }
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public void postPermittableGroups(
          final @Nonnull String tenantIdentifier,
          final @Nonnull String identityManagerApplicationName,
          final @Nonnull String identityManagerApplicationUri,
          final @Nonnull String applicationUri)
  {
    final List<PermittableEndpoint> permittables;
    try (final AutoCloseable ignored = applicationCallContextProvider.getApplicationCallGuestContext(tenantIdentifier)) {
      permittables = getPermittables(applicationUri);
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }

    try (final AutoCloseable ignored
                 = applicationCallContextProvider.getApplicationCallContext(tenantIdentifier, identityManagerApplicationName))
    {
      final IdentityManager identityService = applicationCallContextProvider.getApplication(IdentityManager.class, identityManagerApplicationUri);

      final List<PermittableGroup> permittableGroups = getPermittableGroups(permittables);

      permittableGroups.forEach(x -> createOrFindPermittableGroup(identityService, x));
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

  static List<PermittableGroup> getPermittableGroups(final @Nonnull List<PermittableEndpoint> permittables)
  {
    final Map<String, Set<PermittableEndpoint>> groupedPermittables = new HashMap<>();

    permittables.forEach(x -> groupedPermittables.computeIfAbsent(x.getGroupId(), y -> new LinkedHashSet<>()).add(x));

    return groupedPermittables.entrySet().stream()
            .map(entry -> new PermittableGroup(entry.getKey(), entry.getValue().stream().collect(Collectors.toList())))
            .collect(Collectors.toList());
  }

  void createOrFindPermittableGroup(
          final @Nonnull IdentityManager identityService,
          final @Nonnull PermittableGroup permittableGroup) {
    try {
      identityService.createPermittableGroup(permittableGroup);
      logger.info("Group '{}' successfully created in identity service.", permittableGroup.getIdentifier());
    }
    catch (final PermittableGroupAlreadyExistsException groupAlreadyExistsException)
    {
      //if the group already exists, read out and compare.  If the group is the same, there is nothing left to do.
      final PermittableGroup existingGroup = identityService.getPermittableGroup(permittableGroup.getIdentifier());
      if (!existingGroup.getIdentifier().equals(permittableGroup.getIdentifier())) {
        logger.error("Group '{}' already exists, but has a different name{} (strange).", permittableGroup.getIdentifier(), existingGroup.getIdentifier());
      }

      //Compare as sets because I'm not going to get into a hissy fit over order.
      final Set<PermittableEndpoint> existingGroupPermittables = new HashSet<>(existingGroup.getPermittables());
      final Set<PermittableEndpoint> newGroupPermittables = new HashSet<>(permittableGroup.getPermittables());
      if (!existingGroupPermittables.equals(newGroupPermittables)) {
        logger.error("Group '{}' already exists, but has different contents.", permittableGroup.getIdentifier());
      }
    }
    catch (final RuntimeException unexpected)
    {
      logger.error("Creating group '{}' failed.", permittableGroup.getIdentifier(), unexpected);
    }
  }
}
