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
package io.mifos.provisioner.api.v1.domain;

import io.mifos.core.lang.validation.constraints.ValidIdentifier;

import javax.validation.constraints.NotNull;
import java.util.Objects;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class Tenant {
  @ValidIdentifier
  private String identifier;
  @NotNull
  private String name;
  private String description;
  @NotNull
  private CassandraConnectionInfo cassandraConnectionInfo;
  @NotNull
  private DatabaseConnectionInfo databaseConnectionInfo;

  public Tenant() {
    super();
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public CassandraConnectionInfo getCassandraConnectionInfo() {
    return cassandraConnectionInfo;
  }

  public void setCassandraConnectionInfo(CassandraConnectionInfo cassandraConnectionInfo) {
    this.cassandraConnectionInfo = cassandraConnectionInfo;
  }

  public DatabaseConnectionInfo getDatabaseConnectionInfo() {
    return databaseConnectionInfo;
  }

  public void setDatabaseConnectionInfo(DatabaseConnectionInfo databaseConnectionInfo) {
    this.databaseConnectionInfo = databaseConnectionInfo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Tenant tenant = (Tenant) o;
    return Objects.equals(identifier, tenant.identifier) &&
            Objects.equals(name, tenant.name) &&
            Objects.equals(description, tenant.description) &&
            Objects.equals(cassandraConnectionInfo, tenant.cassandraConnectionInfo) &&
            Objects.equals(databaseConnectionInfo, tenant.databaseConnectionInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, name, description, cassandraConnectionInfo, databaseConnectionInfo);
  }

  @Override
  public String toString() {
    return "Tenant{" +
            "identifier='" + identifier + '\'' +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", cassandraConnectionInfo=" + cassandraConnectionInfo +
            ", databaseConnectionInfo=" + databaseConnectionInfo +
            '}';
  }
}
