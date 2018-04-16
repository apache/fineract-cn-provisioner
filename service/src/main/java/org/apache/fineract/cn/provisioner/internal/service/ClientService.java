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
package org.apache.fineract.cn.provisioner.internal.service;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.Result;
import org.apache.fineract.cn.provisioner.internal.repository.ClientEntity;
import java.util.ArrayList;
import java.util.List;
import org.apache.fineract.cn.cassandra.core.CassandraSessionProvider;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientService {

  private final CassandraSessionProvider cassandraSessionProvider;

  @Autowired
  public ClientService(final CassandraSessionProvider cassandraSessionProvider) {
    super();
    this.cassandraSessionProvider = cassandraSessionProvider;
  }

  public List<ClientEntity> fetchAll() {
    final ArrayList<ClientEntity> result = new ArrayList<>();

    final ResultSet clientResult =
        this.cassandraSessionProvider.getAdminSession().execute("SELECT * FROM clients");

    final Mapper<ClientEntity> clientEntityMapper =
        this.cassandraSessionProvider.getAdminSessionMappingManager().mapper(ClientEntity.class);

    final Result<ClientEntity> mappedClientEntities = clientEntityMapper.map(clientResult);
    if (mappedClientEntities != null) {
      result.addAll(mappedClientEntities.all());
    }

    return result;
  }

  public void create(final ClientEntity clientEntity) {
    final Mapper<ClientEntity> clientEntityMapper =
        this.cassandraSessionProvider.getAdminSessionMappingManager().mapper(ClientEntity.class);
    if (clientEntityMapper.get(clientEntity.getName()) != null) {
      throw ServiceException.conflict("Client {0} already exists!", clientEntity.getName());
    }
    clientEntityMapper.save(clientEntity);
  }

  public void delete(final String name) {
    final Mapper<ClientEntity> clientEntityMapper =
        this.cassandraSessionProvider.getAdminSessionMappingManager().mapper(ClientEntity.class);
    clientEntityMapper.delete(name);
  }

  public ClientEntity findByName(final String name) {
    final Mapper<ClientEntity> clientEntityMapper =
        this.cassandraSessionProvider.getAdminSessionMappingManager().mapper(ClientEntity.class);
    final ClientEntity clientEntity = clientEntityMapper.get(name);
    if (clientEntity == null) {
      throw ServiceException.notFound("Client {0} not found!", name);
    }
    return clientEntity;
  }
}
