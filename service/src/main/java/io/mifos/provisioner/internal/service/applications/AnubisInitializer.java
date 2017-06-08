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
import io.mifos.anubis.api.v1.domain.ApplicationSignatureSet;
import io.mifos.anubis.api.v1.domain.Signature;
import io.mifos.provisioner.config.ProvisionerConstants;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

/**
 * @author Myrle Krantz
 */
@Component
public class AnubisInitializer {
  private final ApplicationCallContextProvider applicationCallContextProvider;
  private final Logger logger;

  @Autowired
  public AnubisInitializer(
          final ApplicationCallContextProvider applicationCallContextProvider,
          @Qualifier(ProvisionerConstants.LOGGER_NAME) final Logger logger) {
    this.applicationCallContextProvider = applicationCallContextProvider;
    this.logger = logger;
  }

  public void initializeResources(@Nonnull String tenantIdentifier, @Nonnull String applicationName, @Nonnull String uri) {
    try (final AutoCloseable ignored
                  = this.applicationCallContextProvider.getApplicationCallContext(tenantIdentifier, applicationName))
    {
      final Anubis anubis = this.applicationCallContextProvider.getApplication(Anubis.class, uri);
      anubis.initializeResources();
      logger.info("Anubis initializeResources for tenant '{}' and application '{}' succeeded.",
              tenantIdentifier, applicationName);

    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public ApplicationSignatureSet createSignatureSet(@Nonnull String tenantIdentifier, @Nonnull String applicationName, @Nonnull String uri, @Nonnull String keyTimestamp, @Nonnull Signature signature) {
    try (final AutoCloseable ignored
                 = this.applicationCallContextProvider.getApplicationCallContext(tenantIdentifier, applicationName))
    {
      final Anubis anubis = this.applicationCallContextProvider.getApplication(Anubis.class, uri);
      final ApplicationSignatureSet applicationSignatureSet = anubis.createSignatureSet(keyTimestamp, signature);
      logger.info("Anubis createSignatureSet for tenant '{}' and application '{}' succeeded with signature set '{}'.",
              tenantIdentifier, applicationName, applicationSignatureSet);
      return applicationSignatureSet;

    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
