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

@Table(name = TenantEntity.TABLE_NAME)
public class TenantEntity {

  static final String TABLE_NAME = "tenants";
  static final String IDENTIFIER_COLUMN = "identifier";
  static final String CLUSTER_NAME_COLUMN = "cluster_name";
  static final String CONTACT_POINTS_COLUMN = "contact_points";
  static final String KEYSPACE_NAME_COLUMN = "keyspace_name";
  static final String REPLICATION_TYPE_COLUMN = "replication_type";
  static final String REPLICAS_COLUMN = "replicas";
  static final String NAME_COLUMN = "name";
  static final String DESCRIPTION_COLUMN = "description";
  static final String IDENTITY_MANAGER_APPLICATION_NAME_COLUMN = "identity_manager_application_name";
  static final String IDENTITY_MANAGER_APPLICATION_URI_COLUMN = "identity_manager_application_uri";

  @PartitionKey
  @Column(name = IDENTIFIER_COLUMN)
  private String identifier;
  @Column(name = CLUSTER_NAME_COLUMN)
  private String clusterName;
  @Column(name = CONTACT_POINTS_COLUMN)
  private String contactPoints;
  @Column(name = KEYSPACE_NAME_COLUMN)
  private String keyspaceName;
  @Column(name = REPLICATION_TYPE_COLUMN)
  private String replicationType;
  @Column(name = REPLICAS_COLUMN)
  private String replicas;
  @Column(name = NAME_COLUMN)
  private String name;
  @Column(name = DESCRIPTION_COLUMN)
  private String description;
  @Column(name = IDENTITY_MANAGER_APPLICATION_NAME_COLUMN)
  private String identityManagerApplicationName;
  @Column(name = IDENTITY_MANAGER_APPLICATION_URI_COLUMN)
  private String identityManagerApplicationUri;

  public TenantEntity() {
    super();
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getContactPoints() {
    return contactPoints;
  }

  public void setContactPoints(String contactPoints) {
    this.contactPoints = contactPoints;
  }

  public String getKeyspaceName() {
    return keyspaceName;
  }

  public void setKeyspaceName(String keyspaceName) {
    this.keyspaceName = keyspaceName;
  }

  public String getReplicationType() {
    return replicationType;
  }

  public void setReplicationType(String replicationType) {
    this.replicationType = replicationType;
  }

  public String getReplicas() {
    return replicas;
  }

  public void setReplicas(String replicas) {
    this.replicas = replicas;
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

  public String getIdentityManagerApplicationName() {
    return identityManagerApplicationName;
  }

  public void setIdentityManagerApplicationName(String identityManagerApplicationName) {
    this.identityManagerApplicationName = identityManagerApplicationName;
  }

  public String getIdentityManagerApplicationUri() {
    return identityManagerApplicationUri;
  }

  public void setIdentityManagerApplicationUri(String identityManagerApplicationUri) {
    this.identityManagerApplicationUri = identityManagerApplicationUri;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TenantEntity that = (TenantEntity) o;

    return identifier.equals(that.identifier);

  }

  @Override
  public int hashCode() {
    return identifier.hashCode();
  }
}
