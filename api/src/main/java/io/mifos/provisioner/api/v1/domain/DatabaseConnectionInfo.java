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
import java.util.Objects;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class DatabaseConnectionInfo {

  @NotNull
  private String driverClass;
  @NotNull
  private String databaseName;
  @NotNull
  private String host;
  @NotNull
  private String port;
  @NotNull
  private String user;
  @NotNull
  private String password;

  public DatabaseConnectionInfo() {
    super();
  }

  @Nonnull
  public String getDriverClass() {
    return driverClass;
  }

  public void setDriverClass(@Nonnull final String driverClass) {
    this.driverClass = driverClass;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(final String databaseName) {
    this.databaseName = databaseName;
  }

  @Nonnull
  public String getHost() {
    return host;
  }

  public void setHost(@Nonnull final String host) {
    this.host = host;
  }

  @Nonnull
  public String getPort() {
    return port;
  }

  public void setPort(@Nonnull final String port) {
    this.port = port;
  }

  @Nonnull
  public String getUser() {
    return user;
  }

  public void setUser(@Nonnull final String user) {
    this.user = user;
  }

  @Nonnull
  public String getPassword() {
    return password;
  }

  public void setPassword(@Nonnull final String password) {
    this.password = password;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DatabaseConnectionInfo that = (DatabaseConnectionInfo) o;
    return Objects.equals(driverClass, that.driverClass) &&
            Objects.equals(databaseName, that.databaseName) &&
            Objects.equals(host, that.host) &&
            Objects.equals(port, that.port) &&
            Objects.equals(user, that.user) &&
            Objects.equals(password, that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(driverClass, databaseName, host, port, user, password);
  }

  @Override
  public String toString() {
    return "DatabaseConnectionInfo{" +
            "driverClass='" + driverClass + '\'' +
            ", databaseName='" + databaseName + '\'' +
            ", host='" + host + '\'' +
            ", port='" + port + '\'' +
            ", user='" + user + '\'' +
            ", password='" + password + '\'' +
            '}';
  }
}
