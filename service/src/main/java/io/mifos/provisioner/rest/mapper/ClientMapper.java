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
package io.mifos.provisioner.rest.mapper;


import io.mifos.provisioner.api.v1.domain.Client;
import io.mifos.provisioner.internal.repository.ClientEntity;

public class ClientMapper {

  private ClientMapper() {
    super();
  }

  public static Client map(final ClientEntity clientEntity) {
    final Client client = new Client();
    client.setName(clientEntity.getName());
    client.setDescription(clientEntity.getDescription());
    client.setRedirectUri(clientEntity.getRedirectUri());
    client.setVendor(clientEntity.getVendor());
    client.setHomepage(clientEntity.getHomepage());
    return client;
  }

  public static ClientEntity map(final Client client) {
    final ClientEntity clientEntity = new ClientEntity();
    clientEntity.setName(client.getName());
    clientEntity.setDescription(client.getDescription());
    clientEntity.setRedirectUri(client.getRedirectUri());
    clientEntity.setVendor(client.getVendor());
    clientEntity.setHomepage(client.getHomepage());
    return clientEntity;
  }
}
