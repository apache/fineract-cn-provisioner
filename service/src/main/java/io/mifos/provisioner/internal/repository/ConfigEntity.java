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

@SuppressWarnings("unused")
@Table(name = ConfigEntity.TABLE_NAME)
public class ConfigEntity {

  static final String TABLE_NAME = "config";
  static final String NAME_COLUMN = "name";
  static final String SECRET_COLUMN = "secret";

  @PartitionKey
  @Column(name = NAME_COLUMN)
  private String name;
  @Column(name = SECRET_COLUMN)
  private byte[] secret;

  public ConfigEntity() {
    super();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public byte[] getSecret() {
    return secret;
  }

  public void setSecret(byte[] secret) {
    this.secret = secret;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConfigEntity that = (ConfigEntity) o;

    return name.equals(that.name);

  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
