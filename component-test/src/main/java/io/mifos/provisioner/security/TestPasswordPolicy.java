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
package io.mifos.provisioner.security;

import io.mifos.core.api.context.AutoUserContext;
import io.mifos.core.api.util.ApiConstants;
import io.mifos.provisioner.AbstractServiceTest;
import io.mifos.provisioner.api.v1.domain.AuthenticationResponse;
import io.mifos.provisioner.api.v1.domain.PasswordPolicy;
import io.mifos.provisioner.config.ProvisionerConstants;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.Base64Utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author Myrle Krantz
 */
public class TestPasswordPolicy extends AbstractServiceTest {

  static private String currentPassword = ProvisionerConstants.INITIAL_PWD;

  @Test
  public void shouldUpdatePassword() {
    final PasswordPolicy passwordPolicy = new PasswordPolicy();
    passwordPolicy.setNewPassword(Base64Utils.encodeToString("new-pwd".getBytes()));

    setPasswordPolicy(passwordPolicy);
    currentPassword = Base64Utils.encodeToString("new-pwd".getBytes());

    final AuthenticationResponse authenticate =
        provisioner.authenticate(this.getClientId(), ApiConstants.SYSTEM_SU, currentPassword);

    checkAuthenticationResponse(authenticate);
  }

  @Test
  public void shouldUpdatePasswordExpiration() {
    final PasswordPolicy passwordPolicy = new PasswordPolicy();
    passwordPolicy.setExpiresInDays(10);

    setPasswordPolicy(passwordPolicy);

    final AuthenticationResponse authenticate =
        provisioner.authenticate(this.getClientId(), ApiConstants.SYSTEM_SU, currentPassword);

    checkAuthenticationResponse(authenticate);
  }

  @Test
  public void shouldUpdatePasswordPolicy() {
    final PasswordPolicy passwordPolicy = new PasswordPolicy();
    passwordPolicy.setNewPassword(Base64Utils.encodeToString("new-pwd".getBytes()));
    passwordPolicy.setExpiresInDays(10);

    setPasswordPolicy(passwordPolicy);
    currentPassword = Base64Utils.encodeToString("new-pwd".getBytes());

    final AuthenticationResponse authenticate =
        provisioner.authenticate(this.getClientId(), ApiConstants.SYSTEM_SU, currentPassword);

    checkAuthenticationResponse(authenticate);
  }

  private void setPasswordPolicy(final PasswordPolicy passwordPolicy)
  {
    final AuthenticationResponse authenticate = provisioner.authenticate(this.getClientId(), ApiConstants.SYSTEM_SU, currentPassword);
    try (final AutoUserContext ignore = new AutoUserContext(ApiConstants.SYSTEM_SU, authenticate.getToken())) {
      provisioner.updatePasswordPolicy(ApiConstants.SYSTEM_SU, passwordPolicy);
    }
  }

  private void checkAuthenticationResponse(final AuthenticationResponse authenticate)
  {
    Assert.assertNotNull(authenticate.getToken());

    final String passwordExpiresAt = authenticate.getAccessTokenExpiration();
    Assert.assertNotNull(passwordExpiresAt);
    final LocalDateTime expires
        = LocalDateTime.parse(passwordExpiresAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    Assert.assertTrue(expires.isAfter(LocalDateTime.now(ZoneId.of("UTC"))));
  }
}