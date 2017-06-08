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
package io.mifos.provisioner.tenant;


import io.mifos.core.api.context.AutoSeshat;
import io.mifos.core.api.util.ApiConstants;
import io.mifos.core.api.util.NotFoundException;
import io.mifos.provisioner.AbstractServiceTest;
import io.mifos.provisioner.api.v1.client.DuplicateIdentifierException;
import io.mifos.provisioner.api.v1.domain.AuthenticationResponse;
import io.mifos.provisioner.api.v1.domain.Tenant;
import io.mifos.provisioner.config.ProvisionerConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class TestTenants extends AbstractServiceTest {

  public TestTenants() {
    super();
  }

  private AutoSeshat autoSeshat;

  @Before
  public void before()
  {
    final AuthenticationResponse authentication = provisioner.authenticate(
        this.getClientId(), ApiConstants.SYSTEM_SU, ProvisionerConstants.INITIAL_PWD);
    autoSeshat = new AutoSeshat(authentication.getToken());
  }

  @After
  public void after() throws InterruptedException {
    //provisioner.deleteTenant(Fixture.getCompTestTenant().getIdentifier());
    autoSeshat.close();
  }

  @Test
  public void shouldCreateTenant() throws Exception {
    final Tenant tenant = Fixture.getCompTestTenant();
    provisioner.createTenant(tenant);

    final Tenant tenantCreated = provisioner.getTenant(tenant.getIdentifier());

    Assert.assertNotNull(tenantCreated);
    Assert.assertEquals(tenant.getIdentifier(), tenantCreated.getIdentifier());
    Assert.assertEquals(tenant.getName(), tenantCreated.getName());
    Assert.assertEquals(tenant.getDescription(), tenantCreated.getDescription());
    Assert.assertEquals(tenant.getCassandraConnectionInfo(), tenantCreated.getCassandraConnectionInfo());

  }

  @Test(expected = DuplicateIdentifierException.class)
  public void shouldFailCreateDuplicate() {
    final Tenant tenant = Fixture.getCompTestTenant();
    provisioner.createTenant(tenant);
    provisioner.createTenant(tenant);
  }

  @Test
  public void shouldFindTenant() {
    final Tenant tenant = Fixture.getCompTestTenant();
    provisioner.createTenant(tenant);
    final Tenant foundTenant = provisioner.getTenant(tenant.getIdentifier());
    Assert.assertNotNull(foundTenant);
    Assert.assertEquals(tenant, foundTenant);
  }

  @Test(expected = NotFoundException.class)
  public void shouldFailFindUnknown() {
    provisioner.getTenant("unknown");
  }

  @Test
  public void shouldFetchAll() {
    final Tenant tenant = Fixture.getCompTestTenant();
    provisioner.createTenant(tenant);
    final List<Tenant> tenants = provisioner.getTenants();
    Assert.assertFalse(tenants.isEmpty());
    Assert.assertTrue(tenants.contains(tenant));
  }
}
