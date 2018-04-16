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
package org.apache.fineract.cn.provisioner.application;


import org.apache.fineract.cn.provisioner.AbstractServiceTest;
import org.apache.fineract.cn.provisioner.api.v1.client.DuplicateIdentifierException;
import org.apache.fineract.cn.provisioner.api.v1.domain.Application;
import org.apache.fineract.cn.provisioner.api.v1.domain.AuthenticationResponse;
import org.apache.fineract.cn.provisioner.config.ProvisionerConstants;
import org.apache.fineract.cn.api.context.AutoSeshat;
import org.apache.fineract.cn.api.util.ApiConstants;
import org.apache.fineract.cn.api.util.NotFoundException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestApplications extends AbstractServiceTest {

  private AutoSeshat autoSeshat;

  public TestApplications() {
    super();
  }


  @Before
  public void before()
  {
    final AuthenticationResponse authentication = provisioner.authenticate(
            this.getClientId(), ApiConstants.SYSTEM_SU, ProvisionerConstants.INITIAL_PWD);
    autoSeshat = new AutoSeshat(authentication.getToken());
  }

  @After
  public void after() {
    provisioner.deleteApplication(Fixture.getApplication().getName());
    autoSeshat.close();
  }

  @Test
  public void shouldCreateApplication() {
    final Application application = Fixture.getApplication();
    provisioner.createApplication(application);

    final Application createdApplication = provisioner.getApplication(application.getName());

    Assert.assertNotNull(createdApplication);
    Assert.assertEquals(application.getName(), createdApplication.getName());
    Assert.assertEquals(application.getDescription(), createdApplication.getDescription());
    Assert.assertEquals(application.getVendor(), createdApplication.getVendor());
    Assert.assertEquals(application.getHomepage(), createdApplication.getHomepage());
  }

  @Test
  public void shouldFindApplication() {
    provisioner.createApplication(Fixture.getApplication());
    Assert.assertNotNull(provisioner.getApplication(Fixture.getApplication().getName()));
  }

  @Test
  public void shouldFetchAll() {
    provisioner.createApplication(Fixture.getApplication());
    Assert.assertFalse(provisioner.getApplications().isEmpty());
  }

  @Test(expected = DuplicateIdentifierException.class)
  public void shouldFailCreateDuplicate() {
    provisioner.createApplication(Fixture.getApplication());
    provisioner.createApplication(Fixture.getApplication());
  }

  @Test(expected = NotFoundException.class)
  public void shouldFailFindUnknown() {
    provisioner.getApplication("unknown");
  }

  @Test
  public void shouldDeleteApplication() {
    final Application applicationToDelete = new Application();
    applicationToDelete.setName("deleteme");

    provisioner.createApplication(applicationToDelete);

    try {
      provisioner.getApplication(applicationToDelete.getName());
    } catch (final RuntimeException ignored) {
      Assert.fail();
    }

    provisioner.deleteApplication(applicationToDelete.getName());

    try {
      provisioner.getApplication(applicationToDelete.getName());
      Assert.fail();
    }
    catch (final RuntimeException ex) {
      Assert.assertTrue(ex instanceof NotFoundException);
    }
  }
}
