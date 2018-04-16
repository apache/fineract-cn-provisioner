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
package org.apache.fineract.cn.provisioner.security;


import org.apache.fineract.cn.provisioner.AbstractServiceTest;
import org.apache.fineract.cn.provisioner.api.v1.client.InvalidProvisionerCredentialsException;
import org.apache.fineract.cn.provisioner.api.v1.domain.AuthenticationResponse;
import org.apache.fineract.cn.provisioner.config.ProvisionerConstants;
import org.apache.fineract.cn.api.util.ApiConstants;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.Base64Utils;

public class TestAuthentication extends AbstractServiceTest {

  public TestAuthentication() {
    super();
  }

  @Test
  public void shouldLoginAdmin() {
    final AuthenticationResponse authenticate
        = provisioner.authenticate(this.getClientId(), ApiConstants.SYSTEM_SU, ProvisionerConstants.INITIAL_PWD);
    Assert.assertNotNull(authenticate.getToken());
  }

  @Test(expected = InvalidProvisionerCredentialsException.class)
  public void shouldFailLoginWrongClientId() {
    provisioner.authenticate("wrong-client", ApiConstants.SYSTEM_SU, ProvisionerConstants.INITIAL_PWD);
  }

  @Test(expected = InvalidProvisionerCredentialsException.class)
  public void shouldFailLoginWrongUser() {
    provisioner.authenticate(this.getClientId(), "wrong-user", ProvisionerConstants.INITIAL_PWD);
  }

  @Test(expected = InvalidProvisionerCredentialsException.class)
  public void shouldFailLoginWrongPassword() {
    provisioner.authenticate(this.getClientId(), ApiConstants.SYSTEM_SU, Base64Utils.encodeToString("wrong-pwd".getBytes()));
  }
}
