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
package io.mifos.provisioner.client;

import io.mifos.core.api.context.AutoSeshat;
import io.mifos.core.api.util.ApiConstants;
import io.mifos.core.api.util.NotFoundException;
import io.mifos.provisioner.AbstractServiceTest;
import io.mifos.provisioner.api.v1.client.DuplicateIdentifierException;
import io.mifos.provisioner.api.v1.domain.AuthenticationResponse;
import io.mifos.provisioner.api.v1.domain.Client;
import io.mifos.provisioner.config.ProvisionerConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestClients extends AbstractServiceTest {

  private AutoSeshat autoSeshat;

  public TestClients() {
    super();
  }
  @Before
  public void before()
  {
    final AuthenticationResponse authentication = provisionerService.authenticate(
        this.getClientId(), ApiConstants.SYSTEM_SU, ProvisionerConstants.INITIAL_PWD);
    autoSeshat = new AutoSeshat(authentication.getToken());
  }

  @After
  public void after() {
    provisionerService.deleteClient(Fixture.getCompTestClient().getName());
    autoSeshat.close();
  }

  @Test
  public void shouldCreateClient() {
    final Client client = Fixture.getCompTestClient();

    provisionerService.createClient(client);
    //TODO: add waiting?

    final Client newlyCreatedClient = provisionerService.getClient(client.getName());

    Assert.assertEquals(client.getName(), newlyCreatedClient.getName());
    Assert.assertEquals(client.getDescription(), newlyCreatedClient.getDescription());
    Assert.assertEquals(client.getHomepage(), newlyCreatedClient.getHomepage());
    Assert.assertEquals(client.getVendor(), newlyCreatedClient.getVendor());
    Assert.assertEquals(client.getRedirectUri(), newlyCreatedClient.getRedirectUri());
  }

  @Test(expected = DuplicateIdentifierException.class)
  public void shouldFailCreateClientAlreadyExists() {
    final Client client = new Client();
    client.setName("duplicate-client");

    provisionerService.createClient(client);
    provisionerService.createClient(client);
  }

  @Test
  public void shouldFindClient() {
    provisionerService.createClient(Fixture.getCompTestClient());
    Assert.assertNotNull(provisionerService.getClient(Fixture.getCompTestClient().getName()));
  }

  @Test(expected = NotFoundException.class)
  public void shouldNotFindClientUnknown() {
    provisionerService.getClient("unknown-client");
  }

  @Test
  public void shouldFetchAllClients() {
    Assert.assertFalse(provisionerService.getClients().isEmpty());
  }

  @Test
  public void shouldDeleteClient() {
    final Client clientToDelete = new Client();
    clientToDelete.setName("deleteme");

    provisionerService.createClient(clientToDelete);

    try {
      provisionerService.getClient(clientToDelete.getName());
    } catch (final Exception ex) {
      Assert.fail();
    }

    provisionerService.deleteClient(clientToDelete.getName());

    try {
      provisionerService.getClient(clientToDelete.getName());
      Assert.fail();
    }
    catch (final RuntimeException ex) {
      Assert.assertTrue(ex instanceof NotFoundException);
    }
  }
}