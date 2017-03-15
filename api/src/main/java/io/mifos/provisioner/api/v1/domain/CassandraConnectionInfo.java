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

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class CassandraConnectionInfo {

  @NotNull
  private String clusterName;
  @NotNull
  private String contactPoints;
  @NotNull
  private String keyspace;
  @NotNull
  private String replicationType;
  @NotNull
  private String replicas;

  public CassandraConnectionInfo() {
    super();
  }

  @Nonnull
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(@Nonnull final String clusterName) {
    this.clusterName = clusterName;
  }

  @Nonnull
  public String getContactPoints() {
    return contactPoints;
  }

  public void setContactPoints(@Nonnull final String contactPoints) {
    this.contactPoints = contactPoints;
  }

  @Nonnull
  public String getKeyspace() {
    return keyspace;
  }

  public void setKeyspace(@Nonnull final String keyspace) {
    this.keyspace = keyspace;
  }

  @Nonnull
  public String getReplicationType() {
    return replicationType;
  }

  public void setReplicationType(@Nonnull final String replicationType) {
    this.replicationType = replicationType;
  }

  @Nonnull
  public String getReplicas() {
    return replicas;
  }

  public void setReplicas(@Nonnull final String replicas) {
    this.replicas = replicas;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CassandraConnectionInfo that = (CassandraConnectionInfo) o;

    return clusterName.equals(that.clusterName)
        && contactPoints.equals(that.contactPoints)
        && keyspace.equals(that.keyspace)
        && replicationType.equals(that.replicationType)
        && replicas.equals(that.replicas);

  }

  @Override
  public int hashCode() {
    int result = clusterName.hashCode();
    result = 31 * result + contactPoints.hashCode();
    result = 31 * result + keyspace.hashCode();
    result = 31 * result + replicationType.hashCode();
    result = 31 * result + replicas.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "CassandraConnectionInfo{" +
        "clusterName='" + clusterName + '\'' +
        ", contactPoints='" + contactPoints + '\'' +
        ", keyspace='" + keyspace + '\'' +
        ", replicationType='" + replicationType + '\'' +
        ", replicas='" + replicas + '\'' +
        '}';
  }
}
