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
package io.mifos.provisioner.internal.util;

final class JdbcUrlBuilder {

  enum DatabaseType {
    MARIADB("jdbc:mariadb://");

    private final String prefix;

    DatabaseType(final String prefix) {
      this.prefix = prefix;
    }

    String prefix() {
      return this.prefix;
    }
  }

  private final DatabaseType type;
  private String host;
  private String port;
  private String instanceName;

  private JdbcUrlBuilder(final DatabaseType type) {
    super();
    this.type = type;
  }

  static JdbcUrlBuilder create(final DatabaseType type) {
    return new JdbcUrlBuilder(type);
  }

  JdbcUrlBuilder host(final String host) {
    this.host = host;
    return this;
  }

  JdbcUrlBuilder port(final String port) {
    this.port = port;
    return this;
  }

  JdbcUrlBuilder instanceName(final String instanceName) {
    this.instanceName = instanceName;
    return this;
  }

  String build() {
    switch (this.type) {
      case MARIADB:
        return this.type.prefix()
            + this.host + ":"
            + this.port
            + (this.instanceName != null ? "/" + this.instanceName : "");
      default:
        throw new IllegalArgumentException("Unknown database type '" + this.type.name() + "'");
    }
  }
}