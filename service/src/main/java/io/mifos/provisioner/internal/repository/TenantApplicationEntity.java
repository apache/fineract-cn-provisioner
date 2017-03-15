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
package io.mifos.provisioner.internal.repository;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.Set;

@Table(name = TenantApplicationEntity.TABLE_NAME)
public class TenantApplicationEntity {

  static final String TABLE_NAME = "tenant_applications";
  static final String TENANT_IDENTIFIER_COLUMN = "tenant_identifier";
  static final String ASSIGNED_APPLICATIONS_COLUMN = "assigned_applications";

  @PartitionKey
  @Column(name = TENANT_IDENTIFIER_COLUMN)
  private String tenantIdentifier;
  @Column(name = ASSIGNED_APPLICATIONS_COLUMN)
  private Set<String> applications;

  public TenantApplicationEntity() {
    super();
  }

  public String getTenantIdentifier() {
    return tenantIdentifier;
  }

  public void setTenantIdentifier(String tenantIdentifier) {
    this.tenantIdentifier = tenantIdentifier;
  }

  public Set<String> getApplications() {
    return applications;
  }

  public void setApplications(Set<String> applications) {
    this.applications = applications;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TenantApplicationEntity that = (TenantApplicationEntity) o;

    return tenantIdentifier.equals(that.tenantIdentifier);

  }

  @Override
  public int hashCode() {
    return tenantIdentifier.hashCode();
  }
}
